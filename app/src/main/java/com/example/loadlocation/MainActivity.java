package com.example.loadlocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;


public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    private final int LOCATION_CODE = 1052;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationManager locationManager;
    private double latitude;
    private double longitude;
    private boolean goingBackground = false;
    private LocationRequest locationRequest;

    private Button getLocationBtn;
    private Button openInMapsBtn;
    private ConstraintLayout parentLayout;
    private TextView locationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //hooks
        getLocationBtn = findViewById(R.id.get_location_btn);
        openInMapsBtn = findViewById(R.id.open_maps_btn);
        parentLayout = findViewById(R.id.parentLayout);
        locationText = findViewById(R.id.text_location_value);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //checkPermission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager
                .PERMISSION_GRANTED) {
            checkPermission();
        }

        getLocationBtn.setOnClickListener(v -> checkProvider());

    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Invoked");

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (goingBackground)
            checkProvider();
        goingBackground = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        goingBackground = true;
    }

    private void checkLocationSettings() {

        LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = settingsClient.checkLocationSettings(locationSettingsRequest);
        locationSettingsResponseTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                //start location updates
                Log.d(TAG, "onComplete: location settings satisfied");
                startLocationUpdates();
            } else {
                if (task.getException() instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) task.getException();
                    try {
                        apiException.startResolutionForResult(MainActivity.this, 1001);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for(Location location: locationResult.getLocations()) {

                Log.d(TAG, "onLocationResult: latitude: "+ location.getLatitude() +
                        " longitude: "+ location.getLongitude());
            }
        }
    };

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void checkProvider() {

        if (locationManager != null)
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Snackbar snackbar = Snackbar.make(parentLayout, "Please Enable GPS", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Enable", v -> {
                    Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(gpsIntent);
                });
                snackbar.getView().setBackgroundDrawable(new InsetDrawable(new ColorDrawable(getResources().
                        getColor(R.color.black)), 20));
                snackbar.show();
            } else {
                checkLocationSettings();
            }
    }

//    private void getLastKnownLocation() {
//        Task<Location> lastKnownLocation = fusedLocationProviderClient.getLastLocation();
//
//        lastKnownLocation.addOnCompleteListener(new OnCompleteListener<Location>() {
//            @Override
//            public void onComplete(@NonNull Task<Location> task) {
//
//                if (task.isSuccessful()) {
//                    if (task.getResult() != null) {
//                        latitude = task.getResult().getLatitude();
//                        longitude = task.getResult().getLongitude();
//                    }
//                    Log.d(TAG, "onComplete: latitude : " + latitude + " longitude: " + longitude);
//                    openInMapsBtn.setEnabled(true);
//                    openInMapsBtn.setClickable(true);
//                    locationText.setText(String.valueOf(latitude + ", " + longitude));
//                } else {
//                    Toast.makeText(MainActivity.this, "Error getting location details", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager
                .PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Location Permission Required!")
                        .setMessage("It is necessary to enable location permission to" +
                                " get location details. Do you want to allow ?")
                        .setPositiveButton("Enable", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_CODE));

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_CODE);
            }
        }
    }

    public void gotoGoogleMap(View view) {

        String uri = String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_CODE)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                checkProvider();
            else
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }
}