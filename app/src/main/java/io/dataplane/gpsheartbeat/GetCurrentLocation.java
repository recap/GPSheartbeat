package io.dataplane.gpsheartbeat;

//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.commons.codec.binary.Base64;

public class GetCurrentLocation extends Activity
        implements OnClickListener {

    private LocationManager locationMangaer = null;
    private LocationListener locationListener = null;

    private Button btnGetLocation = null;
    private EditText editLocation = null;
    private EditText statusText = null;
    private ProgressBar pb = null;

    private static final String TAG = "Debug";
    private Boolean flag = false;

    private static final int REQUEST_GPS = 1;
    private static String[] PERMISSIONS_GPS = {Manifest.permission.ACCESS_FINE_LOCATION};
    private static String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};
    private static String[] PERMISSION_NETWORK = {Manifest.permission.ACCESS_NETWORK_STATE};
    private static String[] PERMISSION_INTERNET = {Manifest.permission.INTERNET};


    private static final int REQUEST_INTERNET = 111;
    private static final int REQUEST_STATE = 22;
    private static final int REQUEST_NETWORK_STATE = 2;
    private static final int PERMISSION_ALL = 10;

    private static long INTERVAL = 1800000; //milliseconds
    private static float DISTANCE = 0;  //meters

    private static long INTERVAL_ACTIVE = 1800000; //milliseconds
    private static float DISTANCE_ACTIVE = 0;  //meters

    private static String SECRET = "V2mtDay6trNXX6rc";

    private final static String COMMAND_L_ON = "svc data enable\n ";
    private final static String COMMAND_L_OFF = "svc data disable\n ";
    private final static String COMMAND_SU = "su";

    private boolean liftoff = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        //handle image sharing
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();





        //if you want to lock screen for always Portrait mode
        setRequestedOrientation(ActivityInfo
                .SCREEN_ORIENTATION_PORTRAIT);

        editLocation = (EditText) findViewById(R.id.editTextLocation);
        statusText = (EditText) findViewById(R.id.statusText);

        locationMangaer = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        locationListener = new MyLocationListener();

        //Toast.makeText(getApplicationContext(), "Sample Text", Toast.LENGTH_LONG).show();


        myInit();


    }


    private void myInit(){

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }else{
            start();
        }
    }

    public static void setConnection(boolean enable,Context context){

        String command;
        if(enable)
            command = COMMAND_L_ON;
        else
            command = COMMAND_L_OFF;

        try{
            Process su = Runtime.getRuntime().exec(COMMAND_SU);
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes(command);
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            outputStream.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void start(){

        try {
            Runtime.getRuntime().exec(COMMAND_SU);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "permissions OK");
        //locationMangaer.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, DISTANCE, locationListener);
        locationMangaer.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, INTERVAL, DISTANCE, locationListener);
        liftoff = true;
    }


    public void onToggleClicked(View view) {
        if(!liftoff){
            return;
        }

        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            Log.i(TAG, "Button1 is on!");
            locationMangaer.removeUpdates(locationListener);
            locationMangaer.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_ACTIVE, DISTANCE_ACTIVE, locationListener);

        } else {
            Log.i(TAG, "Button1 is off!");
            locationMangaer.removeUpdates(locationListener);
            locationMangaer.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, INTERVAL, DISTANCE, locationListener);
        }
    }

    @Override
    public void onClick(View v) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults)
    {

        if(requestCode == PERMISSION_ALL){
            start();
        }

        if(requestCode == REQUEST_GPS) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, PERMISSION_NETWORK, REQUEST_NETWORK_STATE);
            }else{
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSION_INTERNET, REQUEST_INTERNET);
                }else {
                    start();
                }
            }
        }

        if(requestCode == REQUEST_NETWORK_STATE){
            Log.v(TAG, "in request permission REQUEST_NETWORK_STATE");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSION_INTERNET, REQUEST_INTERNET);
            }else {
                start();
            }
        }

        if(requestCode == REQUEST_INTERNET){
            Log.v(TAG, "in request permission INTERNET");
            start();
        }

    }


    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }

    /*----------Method to create an AlertBox ------------- */
    protected void alertbox(String title, String mymessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your Device's GPS is Disable")
                .setCancelable(false)
                .setTitle("** Gps Status **")
                .setPositiveButton("Gps On",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // finish the current activity
                                // AlertBoxAdvance.this.finish();
                                Intent myIntent = new Intent(
                                        Settings.ACTION_SECURITY_SETTINGS);
                                startActivity(myIntent);
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    private boolean checkNetwork(){

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            // if no network is available networkInfo will be null
            // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;

    }
    private class HeartBeat extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params){


            String url = "http://dataplane.io/location/"+params[0];
            try {
                new URL(url).openStream();

            } catch (IOException e) {

                e.printStackTrace();
                return "Failed";
            }


            return "Done";

        }

        @Override
        protected  void onPostExecute(String result){
            if(result == "Failed") {
                statusText.setText("Update failed!");
                Toast.makeText(getApplicationContext(), "Update failed!", Toast.LENGTH_LONG).show();
            }else{
                statusText.setText("Update OK");
            }
        }

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected void onProgressUpdate(Void... values){

        }

    }

    /*----------Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {


       // private void writeStream(OutputStream out, String s) throws IOException {
        //    out.write(s.getBytes());
       //     out.flush();
       // }


        @TargetApi(24)
        private void sendInfo(Location loc){
            String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(),
                    Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc
                        .getLongitude(), 1);
                //if (addresses.size() > 0)
                //    System.out.println(addresses.get(0).getLocality());
                cityName = addresses.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }


            DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));
            dateFormatter.setLenient(false);
            Date today = new Date();
            String datetime = dateFormatter.format(today);



            //String hb = datetime + "," + loc.getLongitude() + "," + loc.getLatitude() + "," + cityName + "," + SECRET;
            String hb = datetime + "," + loc.getLongitude() + "," + loc.getLatitude() + "," + loc.getAltitude() + "," + SECRET;
            String tb = datetime + "\n" + loc.getLongitude() + "\n" + loc.getLatitude(); //"\n"+loc.getAltitude()+"\n"+loc.getSpeed();

            byte[] encoded_str = Base64.encodeBase64(hb
                    .getBytes());
            String encoded_string = new String(encoded_str)
                    .trim();

            new HeartBeat().execute(encoded_string);

            editLocation.setText(tb);
            Toast.makeText(getApplicationContext(), tb, Toast.LENGTH_LONG).show();

        }

        @Override
        @TargetApi(24)
        public void onLocationChanged(Location loc) {



    /*----------to get City-Name from coordinates ------------- */

            //Log.v(TAG, encoded_string);
            Log.v(TAG,"Provider: "+loc.getProvider());

            if (!checkNetwork()) {
                // turn on 3G (only root can do this)
                try {
                    setConnection(true, getApplicationContext());
                    Thread.sleep(5000);
                    //new HeartBeat().execute(encoded_string);
                    sendInfo(loc);
                    Thread.sleep(10000);
                    setConnection(false, getApplicationContext());


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                //new HeartBeat().execute(encoded_string);
                sendInfo(loc);


            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }
}