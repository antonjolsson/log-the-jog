package com.antware.joggerlogger;

public class Walking extends ExerciseType {

    private final static String NAME = "Walking";

    @Override
    String getName() {
        return NAME;
    }

    @Override
    double getMet(double speed, double grade) {
        return 0;
    }
}
