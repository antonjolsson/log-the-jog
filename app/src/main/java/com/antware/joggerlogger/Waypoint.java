package com.antware.joggerlogger;

import android.location.Location;

public class Waypoint {
    private Location location;
    private LogViewModel.ExerciseStatus status;
    private long timeStamp;
    private boolean accountedFor = true;

    Waypoint(Location location, LogViewModel.ExerciseStatus status, long timeStamp) {
        this.location = location;
        this.status = status;
        this.timeStamp = timeStamp;
    }

    public Location getLocation() {
        return location;
    }

    public LogViewModel.ExerciseStatus getStatus() {
        return status;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setAccountedFor(boolean accountedFor) {
        this.accountedFor = accountedFor;
    }

    public boolean isAccountedFor() {
        return accountedFor;
    }

    public void setStatus(LogViewModel.ExerciseStatus status) {
        this.status = status;
    }

    public void setTimestamp(long timestamp) {
        this.timeStamp = timestamp;
    }

}
