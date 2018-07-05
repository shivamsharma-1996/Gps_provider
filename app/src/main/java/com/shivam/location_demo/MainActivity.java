package com.shivam.location_demo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    //private static int DISPLACEMENT = 10; // 10 meters

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private final static int  REQUEST_CHECK_SETTINGS = 108;

    private LocationRequest mLocationRequest;

    private LocationCallback mLocationCallback;
    LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                if (locationResult == null)
                {
                    return;
                }
                for (Location location : locationResult.getLocations())
                {
                    // Update UI with location data
                    Log.i(TAG, String.valueOf(location.getLatitude()));
                    Log.i(TAG, String.valueOf(location.getLongitude()));
                    Toast.makeText(MainActivity.this, "Lat:" + location.getLatitude() + "\n" + "Long" + location.getLongitude(), Toast.LENGTH_LONG).show();
                }
            }
        };
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }



    @Override
    protected void onStart() {
        super.onStart();
        if (checkPlayServices())
        {
            if (!checkPermissions())
            {
                requestPermissions();
            } else
            {
                checkLocationSetting();
            }
        }
    }

    public boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

        //shouldProvideRationale contains true if user previously denied requested permission
        if (shouldProvideRationale)
        {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            startLocationPermissionRequest();
        } else
        {
            //shouldProvideRationale contains false if user ne abhi tak ek bar bhi  permission deny nhi ki ho.
            startLocationPermissionRequest();
        }
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_PERMISSIONS_REQUEST_CODE:
            {
                if (grantResults.length <= 0)
                {
                    //If user interaction was interupted, the permission request is cancelled & you
                    // recieve empty arrays.
                    Log.i(TAG, "User interaction was cancelled");
                }
                else if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    checkLocationSetting();
                }
                else
                {
                    //Permission denied.
                    showSnackBar(R.string.textwarn, R.string.settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
                }
            }
            break;
        }
    }

    private void showSnackBar(final int mainTextStringId, final int actionStringId, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }





    public void checkLocationSetting() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(getLocationRequest());

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

       /* LocationSettingsResponse locationSettingsResponse = new LocationSettingsResponse();
       Log.i("locSettings", String.valueOf(locationSettingsResponse.getLocationSettingsStates())) ;*/

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                Log.i("TAG", "Location mode + GPS is ON");


                //showAccurateGPSLocation();
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                if (e instanceof ResolvableApiException)
                {
                    try
                    {
                        Log.i("TAG", "(Location mode + GPS) is OFF");
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx)
                    {
                        Toast.makeText(MainActivity.this, "GPS Exception", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CHECK_SETTINGS)
        {
            if (resultCode == RESULT_OK)
            {
                startLocationUpdates();
            }
            else if(resultCode == RESULT_CANCELED)
            {
                Toast.makeText(MainActivity.this, "GPS Settings request to on it,is cancelled by user", Toast.LENGTH_LONG).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    protected LocationRequest getLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters
        return  mLocationRequest;
    }
    private void showAccurateGPSLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(lastKnownLocation!=null)
        {
            Log.i("lastKnownLocation", String.valueOf(lastKnownLocation));
        }
        else
        {
            Log.i("lastKnownLocation", "GPS_PROVIDER not enabled");   //but cause ye method call tabhi ho rha h jb gps enabled hoga only so else case not needed
        }
    }






    @Override
    protected void onResume()
    {
        super.onResume();
      //  startLocationUpdates();
    }
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        else
        {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
       stopLocationUpdates();
    }
    private void stopLocationUpdates()
    {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else
            {
                Toast.makeText(getApplicationContext(), "This device is not supported.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

}




