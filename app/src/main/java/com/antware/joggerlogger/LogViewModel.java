package com.antware.joggerlogger;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

    private long totalDuration;
    private long durationBeforePause;
    private Timer timer;
    private long timerStartTime;

    private MutableLiveData<Duration> duration = new MutableLiveData<>(new Duration(0,
            0, 0));
    private MutableLiveData<Double> distanceKm = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> currSpeed = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> avgSpeed = new MutableLiveData<>(0.0);
    private MutableLiveData<Duration> pace = new MutableLiveData<>(new Duration(0, 0,
            0));
    private MutableLiveData<ExerciseStatus> statusLiveData = new MutableLiveData<>(status);

    public ExerciseStatus getExerciseStatus() {
        return status;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
    }

    private void startTimeTaking() {
        timerStartTime = System.currentTimeMillis();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                update();
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, durationBeforePause % 1000, 1000);
    }

    private void update() {
        totalDuration = System.currentTimeMillis() - timerStartTime + durationBeforePause;
        duration.postValue(getDurationFromMs(totalDuration));
        if (waypoints.size() < 2) return;
        setDistance();
        setCurrSpeed(waypoints.getLast());
        setAvgSpeed();
        setPace();
        Log.d("VM", "Status: " + status.toString() + ", Waypoints: " + waypoints.size());
    }

    private void setPace() {
        if (distanceKm.getValue() == null) return;
        long msPerKm = (long) (totalDuration / distanceKm.getValue() + 0.5);
        Duration paceDuration = getDurationFromMs(msPerKm);
        pace.postValue(paceDuration);
    }

    public boolean exerciseJustStarted() {
        return waypoints.size() == 1 || (waypoints.getSecondLast().getStatus() != STARTED &&
                waypoints.getSecondLast().getStatus() != RESUMED);
    }

    public void startButtonPressed() {
        if (status == STARTED || status == RESUMED) {
            status = PAUSED;
            pauseTimeTaking();
            if (!waypoints.isEmpty()) waypoints.getLast().setStatus(status);
            statusLiveData.setValue(status);
        }
        else {
            if (status == STOPPED || status == STOPPED_AFTER_PAUSED) reset();
            startMeasuring();
        }
        Log.d("VM", "Status = " + status.toString() + ", totalDuration = " +
                totalDuration + ", durationBeforePause = " + durationBeforePause);
    }

    void reset() {
        totalDuration = 0;
        durationBeforePause = 0;
        //waypoints = new WaypointList();
        waypoints.clear();
        duration.setValue(new Duration(0, 0, 0));
        distanceKm.setValue(0.0);
        currSpeed.setValue(0.0);
        avgSpeed.setValue(0.0);
    }

    private void pauseTimeTaking() {
        durationBeforePause += System.currentTimeMillis() - timerStartTime;
        timer.cancel();
    }

    public void stopButtonPressed() {
        status = STOPPED_AFTER_PAUSED;
        if (!waypoints.isEmpty())
            waypoints.getLast().setStatus(status);
        statusLiveData.setValue(status);
    }

    private void startMeasuring() {
        status = status == PAUSED ? RESUMED : STARTED;
        statusLiveData.setValue(status);
        startTimeTaking();
    }

    private double getSpeed(int numWaypoints) {
        double speedCalcDistance = 0;
        long speedCalcDuration = 0;
        for (int i = 0; i < numWaypoints && i < waypoints.size() - 1; i++) {
            Waypoint w1 = waypoints.get(waypoints.size() - 2 - i);
            if (w1.getStatus() != STARTED && w1.getStatus() != RESUMED) continue;
            Waypoint w2 = waypoints.get(waypoints.size() - 1 - i);
            //double distanceW1W2 = getDistanceBetweenCoords(w2, w1);
            double distanceW1W2 = w2.getLocation().distanceTo(w1.getLocation()) / 1000.0f;
            speedCalcDuration += w2.getTimeStamp() - w1.getTimeStamp();
            speedCalcDistance += Double.isNaN(distanceW1W2) ? 0 : distanceW1W2;
        }
        double speed = speedCalcDistance / (speedCalcDuration / (LOCATION_UPDATE_FREQ * 60.0 * 60));
        return speed >= 0 ? speed : 0;
    }

    private void setCurrSpeed(Waypoint waypoint) {
        double speed = getSpeed(SECONDS_IN_SPEED_CALC);
        currSpeed.postValue(speed);
        waypoint.setCurrentSpeed(speed);
    }

    private void setAvgSpeed() {
        avgSpeed.postValue(getSpeed(waypoints.size()));
    }

    private void setDistance() {
        if (waypoints.getLast().getLocation() == null || waypoints.getSecondLast().getLocation() == null)
            return;
        //double newDistance = getDistanceBetweenCoords(waypoints.getLast(), waypoints.getSecondLast());
        double newDistance = waypoints.getLast().getLocation().distanceTo(waypoints.getSecondLast().
                getLocation()) / 1000.0;
        if (Double.isNaN(newDistance)) return;
        double oldDistance = distanceKm.getValue() != null ? distanceKm.getValue() : 0;
        Log.d("VM", "Leg distance, m: " + newDistance * 1000);
        distanceKm.postValue(newDistance + oldDistance);
    }

    @NotNull
    public Duration getDurationFromMs(long duration) {
        return new Duration((int) (duration / 1000 / 60 / 60),(int) (duration / 1000 / 60 % 60),
                (int) (duration / 1000 % 60 % 60));
    }

    public MutableLiveData<Double> getCurrSpeed() {
        return currSpeed;
    }

    public LiveData<Double> getAvgSpeed() { return avgSpeed; }

    public LiveData<Duration> getDuration() {
        return duration;
    }

    public LiveData<Double> getDistance() {
        return distanceKm;
    }

    public LiveData<ExerciseStatus> getStatus() {
        return statusLiveData;
    }

    public LiveData<Duration> getPace() {
        return pace;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
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

}
