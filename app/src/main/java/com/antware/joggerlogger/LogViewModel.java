package com.antware.joggerlogger;

import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.PAUSED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STARTED;

public class LogViewModel extends ViewModel {

    Timer timer;

    enum ExerciseStatus {STARTED, STOPPED, PAUSED}
    ExerciseStatus status = ExerciseStatus.STOPPED;

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

    public void startButtonPressed() {
        if (status == ExerciseStatus.STOPPED) {
            startEndTimes = new ArrayList<>();
            startMeasuring();
        }
        else {
            status = ExerciseStatus.STOPPED;
            stopMeasuring();
        }
    }

    private void stopMeasuring() {
        startEndTimes.add(SystemClock.elapsedRealtime());
        timer.cancel();
    }

    private void startMeasuring() {
        status = STARTED;
        startEndTimes.add(SystemClock.elapsedRealtime());
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setDuration();
                setDistance();
                setSpeed();
            }
        }, 0, 100);
    }

    private void setSpeed() {

    }

    private void setDistance() {
    }

    private void setDuration() {
        long durationMs = 0;
        for (int i = 0; i < startEndTimes.size(); i++) {
            if (i % 2 == 1)
                durationMs += startEndTimes.get(i) - startEndTimes.get(i - 1);
        }
        if (status == STARTED)
            durationMs += SystemClock.elapsedRealtime() - startEndTimes.get(startEndTimes.size() - 1);
        duration.postValue(new Duration((int) (durationMs / 1000 / 60 / 60),(int) (durationMs / 1000 / 60 % 60),
                (int) (durationMs / 1000 % 60 % 60)));
    }

    public void pauseButtonPressed() {
        if (status == STARTED) {
            status = PAUSED;
            stopMeasuring();
        }
        else {
            status = STARTED;
            startMeasuring();
        }
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

}
