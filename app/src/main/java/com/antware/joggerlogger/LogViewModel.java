package com.antware.joggerlogger;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.PAUSED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.STARTED;

public class LogViewModel extends ViewModel {

    enum ExerciseStatus {STARTED, STOPPED, PAUSED}
    ExerciseStatus status = ExerciseStatus.STOPPED;

    public ExerciseStatus getExerciseStatus() {
        return status;
    }

    public static class Duration {
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
    }

    private MutableLiveData<Duration> duration = new MutableLiveData<>(new Duration());
    private MutableLiveData<List<Long>> startEndTimes = new MutableLiveData<>
            (new ArrayList<>(Collections.singletonList(0L)));
    private MutableLiveData<Integer> distanceMeters = new MutableLiveData<>(0);
    private MutableLiveData<Double> speed = new MutableLiveData<>(0.0);

    public void startButtonPressed() {
        if (status == ExerciseStatus.STOPPED) {
            status = STARTED;
        }
        else status = ExerciseStatus.STOPPED;
    }

    public void pauseButtonPressed() {
        status = status == STARTED ? PAUSED : STARTED;
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

    public MutableLiveData<List<Long>> getStartEndTimes() {
        return startEndTimes;
    }

}
