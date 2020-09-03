package com.antware.joggerlogger;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class AltitudesHolder {

    public AltitudesHolder(int capacity) {
        this.capacity = capacity;
    }

    private int capacity;
    private Queue<Double> queue = new ArrayDeque<>();
    private int size = 0;
    private double average = 0;

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

    public double getAverage() {
        return average;
    }

    public int getSize() {
        return size;
    }

}
