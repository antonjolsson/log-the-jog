package com.antware.joggerlogger;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.antware.joggerlogger.egm96.Egm96Location;
import com.antware.joggerlogger.egm96.Geoid;
import com.google.android.gms.maps.model.LatLng;

import org.jetbrains.annotations.Nullable;

public class Waypoint implements Parcelable {
    private final static String TAG = "Waypoint";
    private Location location;
    private LogViewModel.ExerciseStatus status;
    private double currentSpeed;

    Waypoint(Location location, LogViewModel.ExerciseStatus status) {
        this.location = location;
        this.status = status;
    }

    protected Waypoint(Parcel in) {
        location = in.readParcelable(Location.class.getClassLoader());
        status = LogViewModel.ExerciseStatus.values()[in.readInt()];
        currentSpeed = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(location, flags);
        dest.writeInt(status.ordinal());
        dest.writeDouble(currentSpeed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Waypoint> CREATOR = new Creator<Waypoint>() {
        @Override
        public Waypoint createFromParcel(Parcel in) {
            return new Waypoint(in);
        }

        @Override
        public Waypoint[] newArray(int size) {
            return new Waypoint[size];
        }
    };

    public static double distanceBetween(Waypoint w1, Waypoint w2) {
        return w2.location.distanceTo(w1.location);
    }

    public LogViewModel.ExerciseStatus getStatus() {
        return status;
    }

    public long getTime() {
        return location.getTime();
    }

    public void setStatus(LogViewModel.ExerciseStatus status) {
        this.status = status;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
        Log.d(TAG, "Currentspeed: " + currentSpeed + ", location.speed: " + location.getSpeed());
    }

    public double getAltitude() {
        Geoid.init();
        double altitude = location.getAltitude() + Geoid.getOffset(new Egm96Location(getLatitude(),
                getLongitude()));
        return altitude;
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public double getLongitude() {
        return location.getLongitude();
    }

    @Nullable
    public LatLng getLatLng() {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public double getLocBasedSpeedMeters() {
        return location.getSpeed();
    }
}
