package com.antware.joggerlogger;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.PAUSED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.RESUMED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STARTED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STOPPED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STOPPED_AFTER_PAUSED;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class LogViewModel extends ViewModel {

    private static final double EARTH_RADIUS = 6371;
    private static final int SECONDS_IN_AVG_SPEED_CALC = 5;
    private static final long LOCATION_UPDATE_FREQ = 1000;
    private static final boolean USE_OWN_SPEED_COMPUTATION = false;
    private static final double M_PER_S_TO_KM_PER_S_COEFF = 3.6;

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

        @NotNull
        public static Duration getDurationFromMs(long duration) {
            return new Duration((int) (duration / 1000 / 60 / 60),(int) (duration / 1000 / 60 % 60),
                    (int) (duration / 1000 % 60 % 60));
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
        duration.postValue(Duration.getDurationFromMs(totalDuration));
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
        Duration paceDuration = Duration.getDurationFromMs(msPerKm);
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
        if (USE_OWN_SPEED_COMPUTATION) {
            return getCalculatedSpeed(numWaypoints);
        }
        else if (numWaypoints == SECONDS_IN_AVG_SPEED_CALC)
            return waypoints.getLast().getLocBasedSpeedMeters() * M_PER_S_TO_KM_PER_S_COEFF;
        else return getLocationBasedAvgSpeed(numWaypoints);
    }

    private double getLocationBasedAvgSpeed(int numWaypoints) {
        double totalSpeed = 0;
        for (Waypoint waypoint : waypoints) {
            totalSpeed += waypoint.getLocBasedSpeedMeters() * M_PER_S_TO_KM_PER_S_COEFF;
        }
        return totalSpeed / numWaypoints;
    }

    private double getCalculatedSpeed(int numWaypoints) {
        double speedCalcDistance = 0;
        long speedCalcDuration = 0;
        for (int i = 0; i < numWaypoints && i < waypoints.size() - 1; i++) {
            Waypoint w1 = waypoints.get(waypoints.size() - 2 - i);
            if (w1.getStatus() != STARTED && w1.getStatus() != RESUMED) continue;
            Waypoint w2 = waypoints.get(waypoints.size() - 1 - i);
            double distanceW1W2 = Waypoint.distanceBetween(w1, w2) / 1000.0f;
            speedCalcDuration += w2.getTime() - w1.getTime();
            speedCalcDistance += Double.isNaN(distanceW1W2) ? 0 : distanceW1W2;
        }
        double speed = speedCalcDistance / (speedCalcDuration / (LOCATION_UPDATE_FREQ * 60.0 * 60));
        return speed >= 0 ? speed : 0;
    }

    private void setCurrSpeed(Waypoint waypoint) {
        double speed = getSpeed(SECONDS_IN_AVG_SPEED_CALC);
        currSpeed.postValue(speed);
        waypoint.setCurrentSpeed(speed);
    }

    private void setAvgSpeed() {
        avgSpeed.postValue(getSpeed(waypoints.size()));
    }

    private void setDistance() {
        double newDistance = Waypoint.distanceBetween(waypoints.getSecondLast(), waypoints.getLast())
                / 1000.0;
        if (Double.isNaN(newDistance)) return;
        double oldDistance = distanceKm.getValue() != null ? distanceKm.getValue() : 0;
        Log.d("VM", "Leg distance, m: " + newDistance * 1000);
        distanceKm.postValue(newDistance + oldDistance);
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

    public WaypointList getWaypoints() {
        return waypoints;
    }

    private double getDistanceBetweenCoords(Waypoint w2, Waypoint w1) {
        if (w1 == null) return Double.NaN;
        double latW1 = toRadians(w1.getLatitude());
        double longW1 = toRadians(w1.getLongitude());
        double latW2 = toRadians(w2.getLatitude());
        double longW2 = toRadians(w2.getLongitude());
        double centralAngle = acos(sin(latW1) * sin(latW2) + cos(latW1) * cos(latW2) * cos(abs(longW1 - longW2)));
        return EARTH_RADIUS * centralAngle;
    }


}
