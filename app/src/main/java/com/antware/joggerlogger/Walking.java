package com.antware.joggerlogger;

/**
 * Class representing the ExerciseType Walking. Useful for e.g. computing calories burned. Feature
 * not yet implemented.
 * @author Anton J Olsson
 */
public class Walking extends ExerciseType {

    private final static String NAME = "Walking";

    @Override
    String getName() {
        return NAME;
    }

    /**
     * Returns the basic metabolic rate depending on speed and ground grade.
     */
    @Override
    double getMet(double speed, double grade) {
        return 0;
    }
}
