package com.antware.joggerlogger;

import android.location.Location;

import java.util.List;

public class ExerciseDetails {

    private static ExerciseDetails exerciseDetails;
    private String fileName;
    private LogViewModel.Duration duration;
    private Double distance;
    private LogViewModel.Duration pace;
    private Double avgSpeed;
    private WaypointList waypoints;
    private Waypoint waypoint;
    private Location location;

    private ExerciseDetails() {}

    public static ExerciseDetails getInstance() {
        if (exerciseDetails == null)
            exerciseDetails = new ExerciseDetails();
        return exerciseDetails;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setDuration(LogViewModel.Duration duration) {
        this.duration = duration;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public void setPace(LogViewModel.Duration pace) {
        this.pace = pace;
    }

    public LogViewModel.Duration getPace() {
        return pace;
    }

    public void setAvgSpeed(Double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public Double getAvgSpeed() {
        return avgSpeed;
    }

    public void setWaypoints(WaypointList waypoints) {
        this.waypoints = waypoints;
    }

    public WaypointList getWaypoints() {
        return waypoints;
    }

    public void setWaypoint(Waypoint waypoint) {
        this.waypoint = waypoint;
    }

    public Waypoint getWaypoint() {
        return waypoint;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
