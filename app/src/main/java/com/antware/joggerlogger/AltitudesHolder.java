package com.antware.joggerlogger;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * Class holding the latest altitudes received.
 * @author Anton J Olsson
 */
public class AltitudesHolder {

    public AltitudesHolder(int capacity) {
        this.capacity = capacity;
    }

    private int capacity;
    private Queue<Double> queue = new ArrayDeque<>();
    private int size = 0;
    private double average = 0;

    /**
     * Adds an altitude to this instance, removing the oldest object in the queue if size > capacity.
     * Then computes average of altitudes in the queue. Class could be made generic and reused in other contexts.
     * @param d the altitude
     */
    public void add(double d) {
        queue.add(d);
        if (++size > capacity){
            queue.poll();
            size--;
        }
        Double[] ds = new Double[size];
        double sum = 0;
        for (double altitude : queue.toArray(ds)) {
            sum += altitude;
        }
        average = sum / size;
    }

    /**
     * Returns the average of the altitudes.
     * @return the average
     */
    public double getAverage() {
        return average;
    }

    /**
     * Returns the number of altitudes.
     * @return the number of altitudes
     */
    public int getSize() {
        return size;
    }

}
