package com.antware.joggerlogger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

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
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MyLocation2 {

    private static final int REQUEST_CHECK_SETTINGS = 1;
    public static final int REQUEST_LOCATION = 2;
    private FusedLocationProviderClient fusedLocationClient;
    private FragmentActivity mainActivity;
    private Location currentLocation;
    private LocationCallback locationCallback;
    private BestLocationResult bestLocationResult;
    private static final int LOCATION_UPDATE_FREQ = 1000;

    public MyLocation2(FragmentActivity mainActivity) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
        this.mainActivity = mainActivity;
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    bestLocationResult.gotLocation(location);
                    return; // TODO: find most accurate location
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void getLocation(@Nullable Context context, @NotNull BestLocationResult bestLocationResult) {
        assert context != null;
        this.bestLocationResult = bestLocationResult;
        fusedLocationClient.getLastLocation().addOnSuccessListener(mainActivity, location -> {
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                bestLocationResult.gotLocation(location);
            }
        });
        createLocationRequest();
    }

    private boolean locationPermitted(@NotNull Context context) {
        return ActivityCompat.checkSelfPermission(context,
            ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context,
                            ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    protected void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_FREQ);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(mainActivity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(mainActivity, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                    Looper.getMainLooper());
        });
        task.addOnFailureListener(mainActivity, getOnFailureListener());
    }

    @NotNull
    private OnFailureListener getOnFailureListener() {
        return e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(mainActivity,
                            REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        };
    }

    abstract static class BestLocationResult {
        abstract void gotLocation(Location location);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

}
