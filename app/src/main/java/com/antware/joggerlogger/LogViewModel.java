package com.antware.joggerlogger;

import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.PAUSED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STARTED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STOPPED;
import static java.lang.Math.*;

public class LogViewModel extends ViewModel {

    private static final double EARTH_RADIUS = 6371;

    public static class Waypoint {
        Location location;
        ExerciseStatus status;
        boolean accountedFor = true;

        public boolean isAccountedFor() {
            return accountedFor;
        }

        Waypoint(Location location, ExerciseStatus status) {
            this.location = location;
            this.status = status;
        }

        Location getLocation() {return location;}
        ExerciseStatus getStatus() {return status;}
        void setAccountedFor(boolean accountedFor) {
            this.accountedFor = accountedFor;
        }
    }

    private List<Waypoint> waypoints = new ArrayList<>();

    public void addWaypoint(Waypoint location) {
        waypoints.add(location);
        setDuration();
        setDistance();
        setSpeed();
    }

    enum ExerciseStatus {STARTED, STOPPED, PAUSED}
    ExerciseStatus status = STOPPED;

    public ExerciseStatus getExerciseStatus() {
        return status;
    }

    public static class Duration {
        int hours, minutes, seconds;
        Duration(int hours, int minutes, int seconds) {
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
        }
    }

    private MutableLiveData<Duration> duration = new MutableLiveData<>(new Duration(0,
            0, 0));
    private List<Long> startEndTimes = new ArrayList<>();
    private MutableLiveData<Double> distanceKm = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> speed = new MutableLiveData<>(0.0);
    private MutableLiveData<ExerciseStatus> statusLiveData = new MutableLiveData<>(status);

    public void startButtonPressed() {
        if (status == ExerciseStatus.STOPPED) {
            startEndTimes = new ArrayList<>();
            startMeasuring();
        }
        else {
            startEndTimes.add(SystemClock.elapsedRealtime());
            status = ExerciseStatus.STOPPED;
            statusLiveData.setValue(status);
        }
    }

    private void startMeasuring() {
        startEndTimes.add(SystemClock.elapsedRealtime());
        status = STARTED;
        statusLiveData.setValue(status);
    }

    private void setSpeed() {

    }

    private void setDistance() {
        if (waypoints.size() < 2) return;
        Waypoint w1 = waypoints.get(waypoints.size() - 2);
        if (w1.status != STARTED) return;
        Waypoint w2 = waypoints.get(waypoints.size() - 1);
        double latW1 = toRadians(w1.getLocation().getLatitude());
        double longW1 = toRadians(w1.getLocation().getLongitude());
        double latW2 = toRadians(w2.getLocation().getLatitude());
        double longW2 = toRadians(w2.getLocation().getLongitude());
        double centralAngle = acos(sin(latW1) * sin(latW2) + cos(latW1) * cos(latW2) * cos(abs(longW1 - longW2)));
        if (Double.isNaN(centralAngle))
            return;
        double newDistance = EARTH_RADIUS * centralAngle;
        double oldDistance = distanceKm.getValue() != null ? distanceKm.getValue() : 0;
        Log.d("VM", "central angle: " + centralAngle);
        Log.d("VM", "distance: " + (newDistance + oldDistance));
        distanceKm.setValue(newDistance + oldDistance);
    }

    private void setDuration() {
        long durationMs = 0;
        for (int i = 0; i < startEndTimes.size(); i++) {
            if (i % 2 == 1)
                durationMs += startEndTimes.get(i) - startEndTimes.get(i - 1);
        }
        if (status == STARTED && startEndTimes.size() > 0)
            durationMs += SystemClock.elapsedRealtime() - startEndTimes.get(startEndTimes.size() - 1);
        duration.postValue(new Duration((int) (durationMs / 1000 / 60 / 60),(int) (durationMs / 1000 / 60 % 60),
                (int) (durationMs / 1000 % 60 % 60)));
    }

    public void pauseButtonPressed() {
        if (status == STARTED) {
            startEndTimes.add(SystemClock.elapsedRealtime());
            status = PAUSED;
            statusLiveData.setValue(status);
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
