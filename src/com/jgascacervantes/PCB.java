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

    public PCB(int[] input){
        this.priority = input[0];
        this.burst = new ArrayList<Integer>();
        this.index = 0;
        for(int i = 1; i < input.length; i++) {
            burst.add(input[i]);
        }

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
