package com.jgascacervantes;

import java.util.ArrayList;

/**
 *
 * Keeps track of stats to calculate performance of the algs
 * Created by jorge on 4/25/17.
 */
public class Statistics {
    public long CPUtime;
    public int throughput;
    public ArrayList<Long> waitTimes;
    public ArrayList<Long> turnaroundT;

    public Statistics() {
        this.CPUtime = 0;
        this.throughput = 0;
        this.waitTimes = new ArrayList<>();
        this.turnaroundT = new ArrayList<>();
    }

    public double calculateWait(){
        double sum = 0.0;
        for(Long i : waitTimes){
            sum += (double)i;
        }
        return sum/(double)waitTimes.size();
    }

    public double calculateTurnaround(){
        double sum = 0.0;
        for(Long i : turnaroundT){
            sum += (double)i;
        }
        return sum/(double)turnaroundT.size();
    }
}
