package com.jgascacervantes;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Represents a Process
 * Created by jorge on 4/23/17.
 */
public class PCB implements Comparable<PCB>{
    public ArrayList<Integer> burst;
    public int priority;
    public int index;
    public long startTime;
    public long endTime;
    public boolean start;

    public PCB(int[] input){
        this.start = true;
        this.priority = input[0];
        this.burst = new ArrayList<>();
        this.index = 0;
        for(int i = 1; i < input.length; i++) {
            burst.add(input[i]);
        }

    }

    public int cpuSum(){
        int sum = 0;
        for(Integer i : burst){
            if(i%2 ==0)
                sum += i;
        }
        return sum;
    }
    public String toString() {
        return this.burst.toString();
    }

    @Override
    public int compareTo(PCB other) {
        if(this.priority > other.priority)
            return 1;
        if( this.priority < other.priority)
            return -1;
        return 0;
    }
}
