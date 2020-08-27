package com.antware.joggerlogger;

import android.location.Location;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import org.jetbrains.annotations.Nullable;

@Entity(tableName = "waypoints")
public class Waypoint {
    @PrimaryKey
    private Location location;
    @ColumnInfo(name = "status")
    private LogViewModel.ExerciseStatus status;
    @ColumnInfo(name = "current_speed")
    private double currentSpeed;

    Waypoint(Location location, LogViewModel.ExerciseStatus status) {
        this.location = location;
        this.status = status;
    }

    public static double distanceBetween(Waypoint w1, Waypoint w2) {
        return w2.location.distanceTo(w1.location);
    }

    /*public Location getLocation() {
        return location;
    }*/

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
    }

    public double getAltitude() {
        return location.getAltitude();
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
