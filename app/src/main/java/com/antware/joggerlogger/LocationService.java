package com.antware.joggerlogger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Class for receiving current location.
 * @author Anton J Olsson
 */
public class LocationService extends Service implements android.location.LocationListener {

    private static final int REQUEST_CHECK_SETTINGS = 1;
    public static final int REQUEST_LOCATION = 2;
    private static final String TAG = LocationService.class.getSimpleName();
    private static final int ONGOING_NOTIFICATION_ID = 3;
    private FusedLocationProviderClient fusedLocationClient;
    private FragmentActivity mainActivity;
    private LocationCallback locationCallback;
    private BestLocationResult bestLocationResult;
    private static final int LOCATION_UPDATE_FREQ = 1000;
    private double lastMslAltitude = Integer.MIN_VALUE;
    private Calendar lastMslAltitudeCalendar;
    private static final int MSL_AVG_ALTITUDE_NUM_ELEMENTS = 60;
    AltitudesHolder altitudesHolder = new AltitudesHolder(MSL_AVG_ALTITUDE_NUM_ELEMENTS);
    private final IBinder serviceBinder = new ServiceBinder();
    private List<Location> locations = new ArrayList<>();
    private boolean saveLocations;
    private LocationManager locationManager;

    /**
     * Save locations to disk or remote database in case the activity is killed and only the service
     * is running. Feature not yet implemented.
     */
    public void setSaveLocations(boolean save) {
        if (save)
            locations.clear();
        saveLocations = save;
    }

    public class ServiceBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    /**
     * Initializes various location providers and locationCallback.
     */
    public void initService(MainActivity mainActivity, Context context){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
        this.mainActivity = mainActivity;
        registerLocationManager(context);
        locationCallback = new LocationCallback() {
            /**
             * Callback for location result from fusedLocationClient. If there are altitudes in
             * altitudesHolder (which are more accurate), sets the location's altitude to their average.
             * More sophisticated method for finding most accurate altitude could be implemented.
             */
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (altitudesHolder.getSize() > 0)
                        location.setAltitude(altitudesHolder.getAverage());
                    bestLocationResult.gotLocation(location);
                    return; // TODO: find most accurate location
                }
            }
        };
    }

    /**
     * Creates notification about application using user's location.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, LocationService.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder = new Notification.Builder(this, createNotificationChannel());
        else builder = new Notification.Builder(this);

        Notification notification = builder
                .setContentTitle(getText(R.string.notification_title))
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    /**
     * Required for Android Oreo.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        NotificationChannel chan = new NotificationChannel("locationService",
                "My Location Service", NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return "locationService";
    }

    @androidx.annotation.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    /**
     * Tries to get last location.
     */
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


    /**
     * Necessary for implementing a LocationListener
     */
    @Override
    public void onLocationChanged(@NonNull Location location) { }

    /**
     * Callback location class.
     */
    abstract static class BestLocationResult {
        abstract void gotLocation(Location location);
    }

    /**
     * Request updates from a LocationManager and add an NmeaListener. This is because Nmea data contains
     * MSL altitude, while FusedLocationProvider returns the ellipsoid height.
     */
    @SuppressLint("MissingPermission")
    public void registerLocationManager(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,
                this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            OnNmeaMessageListener onNmeaMessageListener = (nmea, timestamp) -> parseNmeaString(nmea);
            locationManager.addNmeaListener(onNmeaMessageListener, null);
        }
        else {
            GpsStatus.NmeaListener nmeaListener = (timestamp, nmea) -> parseNmeaString(nmea);
            locationManager.addNmeaListener(nmeaListener);
        }
    }

    /**
     * Parses an Nmea string, if locationManager receives one. Sets the time of the received string,
     * adds the altitude to altitudesHolder and sets lastMslAltitude.
     * Solution mostly taken from https://stackoverflow.com/a/44518339/8773363
     */
    private void parseNmeaString(String line) {
        if (line.startsWith("$")) {
            String[] tokens = line.split(",");
            String type = tokens[0];
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
            }
        }
    }

}
