package com.antware.joggerlogger;

import java.util.ArrayList;

public class WaypointList extends ArrayList<Waypoint> {

    public Waypoint getLast() {
        return this.get(this.size() - 1);
    }

    public Waypoint getSecondLast() {
        return this.get(this.size() - 2);
    }

}
