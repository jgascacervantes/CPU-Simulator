package com.jgascacervantes;


import java.io.*;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    public static void main(String[] args) {
        BufferedReader reader;
        int ALGO_FLAG = 0; //0 = FIFO, 1 = PR, 2 = RR, 3 = SJF
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


        Queue<PCB> readyQueue;
        if(ALGO_FLAG == 1) { //if scheduling algorithm is PR use Priority Queue
            readyQueue = new PriorityQueue<PCB>();
        } else {
            readyQueue = new LinkedList<PCB>();
        }
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
                    if(tokens[0].equals("stop")) {
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
                        readyLock.lock();
                        try {
                            readyQueue.add(process);
                            notEmpty.signal();
                        } finally {
                            readyLock.unlock();
                        }
                    }
                }

            }
        });

        //simulates the io queue
        Thread IOThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!ioQueue.isEmpty() || readerThread.isAlive() ) {
                    ioLock.lock();
                    try {
                        while (ioQueue.isEmpty())
                            notEmptyIO.await();
                        PCB proc;
                        synchronized (ioQueue) {
                            proc = ioQueue.removeFirst();
                        }
                        Thread.sleep(proc.burst.get(proc.index));
                        proc.index++;
                        synchronized (readyQueue) {
                            readyQueue.add(proc);
                        }


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        ioLock.unlock();
                    }
                    readyLock.lock();
                    try {
                        notEmpty.signal();
                    } finally {
                        readyLock.unlock();
                    }
                }
            }
        });

        //simulates the cpu
        Thread CPUThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!readyQueue.isEmpty() || IOThread.isAlive()) {
                    readyLock.lock();
                    try {
                        while (readyQueue.isEmpty())
                            notEmpty.await();
                        PCB proc;
                        synchronized (readyQueue) {
                            proc = ((LinkedList<PCB>) readyQueue).removeFirst();
                        }
                        Thread.sleep(proc.burst.get(proc.index));

                        proc.index++;
                        if (proc.index <= proc.burst.size()) {
                            synchronized (ioQueue) {
                                ioQueue.add(proc);
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        readyLock.unlock();
                    }
                    ioLock.lock();
                    try {
                        notEmptyIO.signal();
                    } finally {
                        ioLock.unlock();
                    }
                }
            }
        });



        readerThread.start();
        CPUThread.start();
        IOThread.start();
        try {
            readerThread.join();
            System.out.println("READER FINISH");
            IOThread.join();
            System.out.println("IO THREAD FINISH");
            CPUThread.join();
            System.out.println("CPU THREAD FINISH");


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }




}
