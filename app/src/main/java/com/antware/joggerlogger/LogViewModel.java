package com.antware.joggerlogger;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    private static final String TAG = "LogViewModel";
    private static final double EARTH_RADIUS = 6371;
    private static final int SECONDS_IN_AVG_SPEED_CALC = 5;
    private static final long LOCATION_UPDATE_FREQ = 1000;
    private static final boolean USE_OWN_SPEED_COMPUTATION = false;
    private static final double M_PER_S_TO_KM_PER_S_COEFF = 3.6;
    private static final String MAP_CIRCLE_RADIUS_TAG = "mapCircleRadius";
    private static final String TIMER_START_TIME_TAG = "timerStartTimeTag";

    private SavedStateHandle savedStateHandle;

    private WaypointList waypoints = new WaypointList();
    private static final String WAYPOINTS_TAG = "waypoints";

    private double mapCircleRadius = 0;

    enum ExerciseStatus {STARTED, STOPPED, PAUSED, RESUMED, STOPPED_AFTER_PAUSED}
    ExerciseStatus status = STOPPED;

    private long totalDuration;
    private static final String TOTAL_DURATION_TAG = "totalDuration";
    private long durationBeforePause;
    private static final String DURATION_BEFORE_PAUSE_TAG = "durationBeforePause";
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

    private Map<String, MutableLiveData<Double>> doubleTags = new HashMap<String, MutableLiveData<Double>>()
            {{ put("distance", distanceKm); put("currSpeed", currSpeed); put("avgSpeed", avgSpeed); }};

    private Map<String, MutableLiveData<Duration>> durationTags = new HashMap<String, MutableLiveData<Duration>>()
            {{ put("duration", duration); put("pace", pace); }};

    public LogViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        if (savedStateHandle.contains("waypoints")) loadState();
    }

    public void saveTimerVars() {
        savedStateHandle.set(TOTAL_DURATION_TAG, totalDuration);
        savedStateHandle.set(DURATION_BEFORE_PAUSE_TAG, durationBeforePause);
        savedStateHandle.set(TIMER_START_TIME_TAG, timerStartTime);
    }

    @SuppressWarnings("ConstantConditions")
    void loadState() {
        if (savedStateHandle.contains(WAYPOINTS_TAG)) {
            waypoints = new WaypointList(savedStateHandle.get(WAYPOINTS_TAG));
            if (!waypoints.isEmpty()){
                status = waypoints.getLast().getStatus();
                statusLiveData.setValue(status);
                if (status == STARTED || status == RESUMED)
                    startTimeTaking();
            }
        }
        if (savedStateHandle.contains(TOTAL_DURATION_TAG))
            totalDuration = savedStateHandle.get(TOTAL_DURATION_TAG);
        if (savedStateHandle.contains(DURATION_BEFORE_PAUSE_TAG))
            durationBeforePause = savedStateHandle.get(DURATION_BEFORE_PAUSE_TAG);
        if (savedStateHandle.contains(TIMER_START_TIME_TAG))
            timerStartTime = savedStateHandle.get(TIMER_START_TIME_TAG);
        if (savedStateHandle.contains(MAP_CIRCLE_RADIUS_TAG))
            mapCircleRadius = savedStateHandle.get(MAP_CIRCLE_RADIUS_TAG);

        for (Map.Entry<String, MutableLiveData<Double>> entry : doubleTags.entrySet()) {
            if (savedStateHandle.contains(entry.getKey()))
                entry.getValue().setValue(savedStateHandle.get(entry.getKey()));
        }
        for (Map.Entry<String, MutableLiveData<Duration>> entry : durationTags.entrySet()) {
            if (savedStateHandle.contains(entry.getKey()))
                entry.getValue().setValue(savedStateHandle.get(entry.getKey()));
        }

        isReloaded = true;
    }

    public ExerciseStatus getExerciseStatus() {
        return status;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        setWaypointSpeed(waypoint);
        savedStateHandle.set(WAYPOINTS_TAG, waypoints);
    }

    private void setWaypointSpeed(Waypoint waypoint) {
        double speed = getSpeed(SECONDS_IN_AVG_SPEED_CALC);
        waypoint.setCurrentSpeed(speed);
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
        setDuration();
        if (USE_OWN_SPEED_COMPUTATION && waypoints.size() < 2) return;
        setCurrSpeed(waypoints.getLast());
        setAvgSpeed();
        setPace();
        if (waypoints.size() > 1) setDistance();
    }

    private void setDuration() {
        Duration duration = Duration.getDurationFromMs(totalDuration);
        this.duration.postValue(duration);
        savedStateHandle.set("duration", duration);
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
        totalDuration = 0L;
        durationBeforePause = 0L;
        timerStartTime = 0L;
        saveTimerVars();
        waypoints.clear();
        savedStateHandle.set(WAYPOINTS_TAG, waypoints);
        setDuration();
        setMapCircleRadius(0);
        setAndSaveLiveData(0, "distance");
        setAndSaveLiveData(0, "currSpeed");
        setAndSaveLiveData(0, "avgSpeed");
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
            return waypoints.size() > 1 ? getComputedSpeed(numWaypoints) : 0.0;
        }
        else return getLocationBasedSpeed(numWaypoints);
    }

    private double getLocationBasedSpeed(int numWaypoints) {
        double totalSpeed = 0;
        int usedWaypoints = Math.min(numWaypoints, waypoints.size());
        for (int i = 0; i < usedWaypoints; i++) {
            Waypoint waypoint = waypoints.get(waypoints.size() - 1 - i);
            totalSpeed += waypoint.getLocBasedSpeedMeters() * M_PER_S_TO_KM_PER_S_COEFF;
        }
        return totalSpeed / Math.min(numWaypoints, waypoints.size());
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
        if (waypoint == null) return;
        double speed = waypoint.getCurrentSpeed();
        setAndSaveLiveData(speed, "currSpeed");
    }

    private void setAndSaveLiveData(double speed, String key) {
        savedStateHandle.set(key, speed);
        Objects.requireNonNull(doubleTags.get(key)).postValue(speed);
    }

    private void setAvgSpeed() {
        double speed = getSpeed(waypoints.size());
        setAndSaveLiveData(speed, "avgSpeed");
    }

    private void setDistance() {
        if (waypoints.getLast() == null || waypoints.getSecondLast() == null) return;
        double newDistance = Waypoint.distanceBetween(waypoints.getSecondLast(), waypoints.getLast())
                / 1000.0;
        if (Double.isNaN(newDistance)) return;
        double oldDistance = distanceKm.getValue() != null ? distanceKm.getValue() : 0.0;
        setAndSaveLiveData(newDistance + oldDistance, "distance");
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

    public double getMapCircleRadius() {
        return mapCircleRadius;
    }

    public void setMapCircleRadius(double mapCircleRadius) {
        this.mapCircleRadius = mapCircleRadius;
        savedStateHandle.set(MAP_CIRCLE_RADIUS_TAG, mapCircleRadius);
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
