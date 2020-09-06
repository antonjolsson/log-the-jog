package com.antware.joggerlogger;

/**
 * Abstract class to be implemented by classes representing various types of exercise (like walking,
 * running, cycling, etc.) and through this computing e.g. calories burned. This feature is not yet implemented.
 * @author Anton J Olsson
 */
public abstract class ExerciseType {

    abstract String getName();

    /**
     * Returns the MET (metabolic rate) for an exercise.
     * @param grade the grade of the ground
     */
    abstract double getMet(double speed, double grade);

}
