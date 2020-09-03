package com.antware.joggerlogger;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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

import java.util.Calendar;
import java.util.TimeZone;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class LocationService extends Service implements android.location.LocationListener {

    private static final int REQUEST_CHECK_SETTINGS = 1;
    public static final int REQUEST_LOCATION = 2;
    private static final String TAG = "LogLocationManager";
    private FusedLocationProviderClient fusedLocationClient;
    private FragmentActivity mainActivity;
    private LocationCallback locationCallback;
    private BestLocationResult bestLocationResult;
    private static final int LOCATION_UPDATE_FREQ = 1000;
    private double lastMslAltitude = Integer.MIN_VALUE;
    private Calendar lastMslAltitudeCalendar;
    private static final long MSL_ALTITUDE_AGE_LIMIT_MS = 10000;
    private static final int MSL_AVG_ALTITUDE_NUM_ELEMENTS = 60;
    AltitudesHolder altitudesHolder = new AltitudesHolder(MSL_AVG_ALTITUDE_NUM_ELEMENTS);
    private final IBinder serviceBinder = new ServiceBinder();

    public class ServiceBinder extends Binder {
        /**
         * Returns this instance.
         * @return this instance
         */
        LocationService getService() {
            return LocationService.this;
        }
    }

    public void initService(MainActivity mainActivity, Context context){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
        this.mainActivity = mainActivity;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            registerLocationManager(context);
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    /*if (lastMslAltitude > Integer.MIN_VALUE && recentLastMslAltitude())*/
                    Log.d(TAG, String.valueOf(altitudesHolder.getAverage()));
                    if (altitudesHolder.getSize() > 0)
                        location.setAltitude(altitudesHolder.getAverage());
                    bestLocationResult.gotLocation(location);
                    return; // TODO: find most accurate location
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @androidx.annotation.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    private boolean recentLastMslAltitude() {
        Calendar nowGMT = Calendar.getInstance();
        return nowGMT.getTimeInMillis() - lastMslAltitudeCalendar.getTimeInMillis() < MSL_ALTITUDE_AGE_LIMIT_MS;
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

    @Override
    public void onLocationChanged(@NonNull Location location) { }

    abstract static class BestLocationResult {
        abstract void gotLocation(Location location);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private OnNmeaMessageListener mNmeaListener = (nmea, timestamp) -> parseNmeaString(nmea);


    @RequiresApi(api = Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    public void registerLocationManager(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,
                this);
        locationManager.addNmeaListener(mNmeaListener, null);
    }

    // Solution mostly taken from https://stackoverflow.com/a/44518339/8773363
    private void parseNmeaString(String line) {
        if (line.startsWith("$")) {
            String[] tokens = line.split(",");
            String type = tokens[0];
            //Log.d(TAG, line);
            // Parse altitude above sea level, Detailed description of NMEA string here http://aprs.gids.nl/nmea/#gga
            if ((type.matches("\\$..GGA.*") && !tokens[9].isEmpty() && !tokens[11].equals("0."))) {
                String timeString = String.valueOf(tokens[1]);
                lastMslAltitudeCalendar = Calendar.getInstance();
                lastMslAltitudeCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                lastMslAltitudeCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeString.substring(0, 2)));
                lastMslAltitudeCalendar.set(Calendar.MINUTE, Integer.parseInt(timeString.substring(2, 4)));
                lastMslAltitudeCalendar.set(Calendar.SECOND, Integer.parseInt(timeString.substring(4, 6)));
                altitudesHolder.add(Double.parseDouble(tokens[9]));
                lastMslAltitude = altitudesHolder.getAverage();
                Log.d(TAG, line);
                Log.d(TAG, "Average altitude: " + altitudesHolder.getAverage());
            }
        }
    }

}
