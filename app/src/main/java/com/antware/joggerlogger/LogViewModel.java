package com.antware.joggerlogger;

import android.location.Location;
import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.PAUSED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STARTED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STOPPED;

public class LogViewModel extends ViewModel {

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
    private MutableLiveData<Integer> distanceMeters = new MutableLiveData<>(0);
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
        double distance; int i;
        //while (!waypoints.get(i).isAccountedFor() && waypoints.get(i))
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

    public LiveData<Integer> getDistance() {
        return distanceMeters;
    }

    public LiveData<ExerciseStatus> getStatus() {
        return statusLiveData;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }
}
