package com.antware.joggerlogger;

public class ExerciseDetails {

    private static ExerciseDetails exerciseDetails;
    private String fileName;
    private Duration duration;
    private Double distance;
    private Duration pace;
    private Double avgSpeed;
    private WaypointList waypoints;
    private Waypoint waypoint;

    private ExerciseDetails() {}

    public static ExerciseDetails getInstance() {
        if (exerciseDetails == null)
            exerciseDetails = new ExerciseDetails();
        return exerciseDetails;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public void setPace(Duration pace) {
        this.pace = pace;
    }

    public Duration getPace() {
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

}
