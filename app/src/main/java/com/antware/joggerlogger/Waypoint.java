package com.antware.joggerlogger;

import android.location.Location;

public class Waypoint {
    private Location location;
    private LogViewModel.ExerciseStatus status;
    private double currentSpeed;

    Waypoint(Location location, LogViewModel.ExerciseStatus status) {
        this.location = location;
        this.status = status;
    }

    public Location getLocation() {
        return location;
    }

    public LogViewModel.ExerciseStatus getStatus() {
        return status;
    }

    public long getTimeStamp() {
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
}
