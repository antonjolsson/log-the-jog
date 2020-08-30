package com.antware.joggerlogger;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

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
