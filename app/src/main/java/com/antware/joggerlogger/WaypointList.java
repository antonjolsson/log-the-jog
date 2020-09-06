package com.antware.joggerlogger;

import java.util.ArrayList;

/**
 * Subclass to easily get last and second Waypoint in an ArrayList with Waypoints.
 * @author Anton J Olsson
 */
public class WaypointList extends ArrayList<Waypoint>{

    public WaypointList(ArrayList<Waypoint> waypoints) {
        this.addAll(waypoints);
    }

    public WaypointList() {}

    public Waypoint getLast() {
        if (this.isEmpty()) return null;
        return this.get(this.size() - 1);
    }

    public Waypoint getSecondLast() {
        if (this.size() < 2) return null;
        return this.get(this.size() - 2);
    }
}
