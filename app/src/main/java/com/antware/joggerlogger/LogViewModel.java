package com.antware.joggerlogger;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
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

    private SavedStateHandle savedStateHandle;

    private WaypointList waypoints = new WaypointList();

    enum ExerciseStatus {STARTED, STOPPED, PAUSED, RESUMED, STOPPED_AFTER_PAUSED}
    ExerciseStatus status = STOPPED;

    private long totalDuration;
    private long durationBeforePause;
    private Timer timer;
    private long timerStartTime;

    private boolean isReloaded = false;

    private MutableLiveData<Duration> duration = new MutableLiveData<>(new Duration(0,
            0, 0));
    private MutableLiveData<Double> distanceKm = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> currSpeed = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> avgSpeed = new MutableLiveData<>(0.0);
    private MutableLiveData<Duration> pace = new MutableLiveData<>(new Duration(0, 0,
            0));
    private MutableLiveData<ExerciseStatus> statusLiveData = new MutableLiveData<>(status);
    private MutableLiveData<Boolean> isReloadedLiveData = new MutableLiveData<>(false);

    public LogViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        if (savedStateHandle.contains("waypoints")) loadState();
    }

    public void saveState() {
        savedStateHandle.set("totalDuration", totalDuration);
        savedStateHandle.set("durationBeforePause", durationBeforePause);
        savedStateHandle.set("timerStartTime", timerStartTime);
        isReloadedLiveData.setValue(false);
        isReloaded = false;
    }

    @SuppressWarnings("ConstantConditions")
    void loadState() {
        if (savedStateHandle.contains("waypoints")) {
            waypoints = new WaypointList(savedStateHandle.get("waypoints"));
            if (!waypoints.isEmpty()){
                status = waypoints.getLast().getStatus();
                statusLiveData.setValue(status);
                if (status == STARTED || status == RESUMED)
                    startMeasuring();
                Log.d("LogViewModel", "Waypoints: " + waypoints.size() + ", Status: " +
                        status);
            }
        }
        if (savedStateHandle.contains("totalDuration"))
            totalDuration = savedStateHandle.get("totalDuration");
        if (savedStateHandle.contains("durationBeforePause"))
            durationBeforePause = savedStateHandle.get("durationBeforePause");
        if (savedStateHandle.contains("timerStartTime"))
            timerStartTime = savedStateHandle.get("timerStartTime");
        if (savedStateHandle.contains("distance"))
            distanceKm.setValue(savedStateHandle.get("distance"));
        if (savedStateHandle.contains("currSpeed"))
            currSpeed.setValue(savedStateHandle.get("currSpeed"));
        if (savedStateHandle.contains("avgSpeed"))
            avgSpeed.setValue(savedStateHandle.get("avgSpeed"));
        isReloadedLiveData.setValue(true);
        isReloaded = true;
    }

    public ExerciseStatus getExerciseStatus() {
        return status;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        savedStateHandle.set("waypoints", (ArrayList<Waypoint>) waypoints);
    }

    private void startTimeTaking() {
        timerStartTime = timerStartTime == 0 ? System.currentTimeMillis() : timerStartTime;
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
    }

    private void setPace() {
        if (distanceKm.getValue() == null) return;
        long msPerKm = (long) (totalDuration / distanceKm.getValue() + 0.5);
        Duration paceDuration = Duration.getDurationFromMs(msPerKm);
        pace.postValue(paceDuration);
        savedStateHandle.set("pace", paceDuration);
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
    }

    void reset() {
        totalDuration = 0;
        durationBeforePause = 0;
        timerStartTime = 0;
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
            return getComputedSpeed(numWaypoints);
        }
        else return getLocationBasedSpeed(numWaypoints);
    }

    private double getLocationBasedSpeed(int numWaypoints) {
        double totalSpeed = 0;
        int usedWaypoints = Math.min(numWaypoints, waypoints.size() - 1);
        for (int i = 0; i < usedWaypoints; i++) {
            Waypoint waypoint = waypoints.get(waypoints.size() - 1 - i);
            totalSpeed += waypoint.getLocBasedSpeedMeters() * M_PER_S_TO_KM_PER_S_COEFF;
        }
        return totalSpeed / Math.min(numWaypoints, waypoints.size() - 1);
    }

    private double getComputedSpeed(int numWaypoints) {
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
        savedStateHandle.set("currSpeed", speed);
    }

    private void setAvgSpeed() {
        double speed = getSpeed(waypoints.size());
        avgSpeed.postValue(speed);
        savedStateHandle.set("avgSpeed", speed);
    }

    private void setDistance() {
        double newDistance = Waypoint.distanceBetween(waypoints.getSecondLast(), waypoints.getLast())
                / 1000.0;
        if (Double.isNaN(newDistance)) return;
        double oldDistance = distanceKm.getValue() != null ? distanceKm.getValue() : 0.0;
        distanceKm.postValue(newDistance + oldDistance);
        savedStateHandle.set("distance", newDistance + oldDistance);
    }

    public LiveData<Double> getCurrSpeed() {
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

    public LiveData<Boolean> getIsReloadedLiveData() { return isReloadedLiveData; }

    public WaypointList getWaypoints() {
        return waypoints;
    }

    public boolean isReloaded() {
        return isReloaded;
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
