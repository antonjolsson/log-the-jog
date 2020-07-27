package com.antware.joggerlogger;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.PAUSED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.RESUMED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STARTED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STOPPED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STOPPED_AFTER_PAUSED;
import static com.antware.joggerlogger.MyLocationKt.*;
import static java.lang.Math.*;

public class LogViewModel extends ViewModel {

    private static final double EARTH_RADIUS = 6371;
    private static final int SECONDS_IN_SPEED_CALC = 5;

    private WaypointList waypoints = new WaypointList();

    enum ExerciseStatus {STARTED, STOPPED, PAUSED, RESUMED, STOPPED_AFTER_PAUSED}
    ExerciseStatus status = STOPPED;

    public static class Duration {
        int hours, minutes, seconds;
        Duration(int hours, int minutes, int seconds) {
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
        }
    }

    private long totalDuration = 0;

    private MutableLiveData<Duration> duration = new MutableLiveData<>(new Duration(0,
            0, 0));
    private MutableLiveData<Double> distanceKm = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> speed = new MutableLiveData<>(0.0);
    private MutableLiveData<ExerciseStatus> statusLiveData = new MutableLiveData<>(status);

    public ExerciseStatus getExerciseStatus() {
        return status;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        if (exerciseJustStarted()){
            if (waypoints.size() == 1) totalDuration = 0;
            return;
        }
        setDuration();
        setDistance();
        setSpeed();
    }

    public boolean exerciseJustStarted() {
        return waypoints.size() == 1 || (waypoints.getSecondLast().getStatus() != STARTED &&
                waypoints.getSecondLast().getStatus() != RESUMED);
    }

    public void startButtonPressed() {
        if (status == STOPPED || status == STOPPED_AFTER_PAUSED) {
            if (waypoints.size() > 1) {
                waypoints = new WaypointList();
                duration.setValue(new Duration(0, 0, 0));
                distanceKm.setValue(0.0);
                speed.setValue(0.0);
            }
            startMeasuring();
        }
        else {
            status = status == PAUSED ? STOPPED_AFTER_PAUSED : STOPPED;
            waypoints.getLast().setStatus(status);
            statusLiveData.setValue(status);
        }
    }

    private void startMeasuring() {
        //status = STARTED;
        status = status == PAUSED ? RESUMED : STARTED;
        statusLiveData.setValue(status);
    }

    private void setSpeed() {
        double speedCalcDistance = 0;
        long speedCalcDuration = 0;
        for (int i = 0; i < SECONDS_IN_SPEED_CALC && i < waypoints.size() - 1; i++) {
            Waypoint w1 = waypoints.get(waypoints.size() - 2 - i);
            if (w1.getStatus() != STARTED && w1.getStatus() != RESUMED) continue;
            Waypoint w2 = waypoints.get(waypoints.size() - 1 - i);
            double distanceW1W2 = getDistanceBetweenCoords(w2, w1);
            speedCalcDuration += w2.getTimeStamp() - w1.getTimeStamp();
            speedCalcDistance += Double.isNaN(distanceW1W2) ? 0 : distanceW1W2;
        }
        speed.setValue(speedCalcDistance / (speedCalcDuration / (LOCATION_UPDATE_FREQ * 60.0 * 60)));
    }

    private double getDistanceBetweenCoords(Waypoint w2, Waypoint w1) {
        if (w1.getLocation() == null) return Double.NaN;
        double latW1 = toRadians(w1.getLocation().getLatitude());
        double longW1 = toRadians(w1.getLocation().getLongitude());
        double latW2 = toRadians(w2.getLocation().getLatitude());
        double longW2 = toRadians(w2.getLocation().getLongitude());
        double centralAngle = acos(sin(latW1) * sin(latW2) + cos(latW1) * cos(latW2) * cos(abs(longW1 - longW2)));
        return EARTH_RADIUS * centralAngle;
    }

    private void setDistance() {
        double newDistance = getDistanceBetweenCoords(waypoints.getLast(), waypoints.getSecondLast());
        if (Double.isNaN(newDistance)) return;
        double oldDistance = distanceKm.getValue() != null ? distanceKm.getValue() : 0;
        Log.d("VM", "Leg distance, m: " + newDistance * 1000);
        distanceKm.setValue(newDistance + oldDistance);
    }

    private void setDuration() {
        totalDuration += waypoints.getLast().getTimeStamp() - waypoints.getSecondLast().getTimeStamp();
        duration.postValue(new Duration((int) (totalDuration / 1000 / 60 / 60),(int) (totalDuration / 1000 / 60 % 60),
                (int) (totalDuration / 1000 % 60 % 60)));
    }

    public void pauseButtonPressed() {
        if (status == STARTED || status == RESUMED) {
            Waypoint pausePoint = waypoints.getLast();
            //pausePoint.setTimestamp(SystemClock.elapsedRealtime());
            pausePoint.setStatus(PAUSED);

            status = PAUSED;
            statusLiveData.setValue(PAUSED);
        }
        else startMeasuring();
    }

    public MutableLiveData<Double> getSpeed() {
        return speed;
    }

    public LiveData<Duration> getDuration() {
        return duration;
    }

    public LiveData<Double> getDistance() {
        return distanceKm;
    }

    public LiveData<ExerciseStatus> getStatus() {
        return statusLiveData;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }
}
