package com.jgascacervantes;

import java.io.*;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    public static void main(String[] args) {
        BufferedReader reader;

        if (args.length == 0) {
            System.out.println("wrong argcount");
            return;
        }
        String inputFilePath = args[args.length-1];

        try {
            reader = new BufferedReader(new FileReader(inputFilePath));
        } catch (FileNotFoundException e){
            System.out.println("File Not Found");
            return;
        }

        PriorityQueue<PCB> readyQueue = new PriorityQueue<PCB>();
        LinkedList<PCB> ioQueue = new LinkedList<PCB>();
        final Lock readyLock = new ReentrantLock();
        final Condition notEmpty = readyLock.newCondition();
        final Lock ioLock = new ReentrantLock();
        final Condition notEmptyIO = ioLock.newCondition();



        //reads inputfile, creates a process object and puts it in queue
        Thread readerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    String line = null;
                    try {
                        line = reader.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String[] tokens = line.split("\\s+");
                    if(tokens[0].equals("stop") || tokens[0].equals(null)) {
                        break;
                    } else if(tokens[0].equals("sleep")) {
                        try {
                            Thread.sleep(Integer.parseInt(tokens[1]));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else if (tokens[0].equals("proc")){
                        int[] bursts = new int[tokens.length-1];
                        for(int i = 1; i < tokens.length; i++){
                            bursts[i -1] = Integer.parseInt(tokens[i]);
                        }
                        PCB process = new PCB(bursts);
                        synchronized (readyQueue) {
                            readyQueue.add(process);
                        }
                    }
                }
            }
        });

        //simulates the cpu
        Thread CPUThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO
            }
        });

        //simulates the io queue
        Thread IOThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO
            }
        });

        readerThread.start();
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }




}
