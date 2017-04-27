package com.jgascacervantes;


import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    public static void main(String[] args) {
        BufferedReader reader;

        final int ALGO_FLAG; //0 = FIFO, 1 = PR, 2 = RR, 3 = SJF
        if (args.length < 3) {
            System.out.println("wrong argcount");
            return;
        }
        final int quantum = Integer.parseInt(args[3]);
        if(args[0].equals("-alg") ){
            switch (args[1]) {
                case "FIFO":    ALGO_FLAG = 0;
                                break;
                case "PR":     ALGO_FLAG = 1;
                                break;
                case "RR":      if(args[2].equals("-quantum")) {
                                    ALGO_FLAG = 2;
                                    break;
                                } else{
                                    System.out.println("USAGE: -alg [FIFO|SJF|PR|RR] [-quantum [integer(ms)]] -input [file name]");
                                    return;
                                }
                case "SJF":     ALGO_FLAG = 3;
                                break;

                default:        System.out.println("USAGE: -alg [FIFO|SJF|PR|RR] [-quantum [integer(ms)]] -input [file name]");
                                return;
            }
        } else {
            System.out.println("USAGE: -alg [FIFO|SJF|PR|RR] [-quantum [integer(ms)]] -input [file name]");
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
        if(ALGO_FLAG == 1 || ALGO_FLAG == 3) { //if scheduling algorithm is PR use Priority Queue
            readyQueue = new PriorityQueue<PCB>();
        } else {
            readyQueue = new LinkedList<PCB>();
        }
        LinkedList<PCB> ioQueue = new LinkedList<PCB>();
        ArrayList<PCB> finalQueue = new ArrayList<>();
        // Ready Queue Lock and Condition variable
        final Lock readyLock = new ReentrantLock();
        final Condition notEmpty = readyLock.newCondition();
        // IO queue Lock and Condition variable
        final Lock ioLock = new ReentrantLock();
        final Condition notEmptyIO = ioLock.newCondition();

        Statistics statistics = new Statistics();

        //reads inputfile, creates a process object and puts it in queue
        Thread readerThread = new Thread(() -> {
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

        });

        //reads input file, changes priorities based on burst time, sends to readyQueue
        Thread SJFreaderThread = new Thread(() -> {
            ArrayList<PCB> processes = new ArrayList<>();
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
                    processes.add(process);
                }
            }
            //sort by CPU burst time
            Collections.sort(processes, (p1, p22) -> {
                if (p1.cpuSum() > p22.cpuSum())
                    return 1;
                if (p1.cpuSum() < p22.cpuSum())
                    return -1;
                return 0;
            });
            int i = 0;
            //edit priority and add to readyQueue
            for(PCB p : processes){
                p.priority = i;
                i++;
                readyLock.lock();
                try {
                    readyQueue.add(p);
                    notEmpty.signal();
                } finally {
                    readyLock.unlock();
                }
            }



        });

        //simulates the io queue
        Thread IOThread = new Thread(() -> {
            while(!ioQueue.isEmpty() || readerThread.isAlive() || SJFreaderThread.isAlive() ) {
                ioLock.lock();
                try {
                    while (ioQueue.isEmpty())
                        notEmptyIO.await();
                    PCB proc;
                    synchronized (ioQueue) {
                        proc = ioQueue.removeFirst();
                    }
                    Thread.sleep(proc.burst.get(proc.index));
                    synchronized (proc) {
                        proc.index++;
                    }
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
        });

        //simulates the cpu
        Thread CPUThread = new Thread(() -> {
            while(!readyQueue.isEmpty() || IOThread.isAlive()) {
                boolean io = false;
                readyLock.lock();
                try {
                    while (readyQueue.isEmpty())
                        notEmpty.await();
                    PCB proc;
                    synchronized (readyQueue) {
                        if(ALGO_FLAG == 1 || ALGO_FLAG == 3){
                            proc = ((PriorityQueue<PCB>) readyQueue).remove();
                        } else
                            proc = ((LinkedList<PCB>) readyQueue).removeFirst();
                    }

                    if(proc.start){ // first time getting the cpu?
                        proc.start = false;
                        long initial = System.nanoTime();
                        proc.startTime = initial;
                    }
                    long start;
                    long end;
                    if(ALGO_FLAG == 2){
                        start = System.nanoTime();
                        int i;
                        for(i = proc.burst.get(proc.index); i > 0 && i > (proc.burst.get(proc.index) - quantum); i-- ) {
                            Thread.sleep(1);
                        }
                        end = System.nanoTime();
                        proc.burst.set(proc.index, i);
                    } else {
                        start = System.nanoTime();
                        Thread.sleep(proc.burst.get(proc.index));
                        end = System.nanoTime();
                        proc.burst.set(proc.index, 0);
                    }
                    statistics.CPUtime += end-start;

                    if(proc.burst.get(proc.index) > 0){
                        synchronized (readyQueue){
                            readyQueue.add(proc);
                        }
                    } else if (proc.index+1 < proc.burst.size()) {
                        synchronized (ioQueue) {
                            synchronized (proc) {
                                proc.index++;
                            }
                            ioQueue.add(proc);
                            io = true;
                        }
                    }
                    statistics.throughput++;
                    proc.endTime = System.nanoTime();
                    finalQueue.add(proc);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    readyLock.unlock();
                }
                if(io) {
                    ioLock.lock();
                    try {
                        notEmptyIO.signal();
                    } finally {
                        ioLock.unlock();
                    }
                } else {
                    readyLock.lock();
                    try{
                        notEmpty.signal();
                    } finally {
                        readyLock.unlock();
                    }
                }
            }
        });
        long startTime;
        if(ALGO_FLAG == 3){
            SJFreaderThread.start();
            startTime = System.nanoTime();
            try {
                SJFreaderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(ALGO_FLAG != 3) {
            readerThread.start();
        }
        startTime = System.nanoTime();
        CPUThread.start();
        IOThread.start();
        try {
            readerThread.join();
            IOThread.join();
            CPUThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();

        //calculations
        long totalTime = endTime - startTime;
        long cpuUsage = statistics.CPUtime * 100 / totalTime;
        double tput = (double)statistics.throughput/((double)totalTime/ 1000000.0);
        tput = Math.floor(tput * 100000)/ 100000;
        for(PCB p : finalQueue){
            Long waitTime = p.startTime - startTime;
            Long turnaroundT = p.endTime - startTime;
            waitTime = TimeUnit.NANOSECONDS.toMillis(waitTime);
            turnaroundT = TimeUnit.NANOSECONDS.toMillis(turnaroundT);
            statistics.waitTimes.add(waitTime);
            statistics.turnaroundT.add(turnaroundT);
        }
        //FINAL OUTPUT
        System.out.println("Input File Name : " + inputFilePath);
        System.out.println("CPU Scheduling Algorithm: " + args[1]);
        System.out.println("CPU Utilization : " + cpuUsage + "%");
        System.out.println("Throughput : " + tput + " Processes per Millisecond");
        System.out.println("Average Turnaround Time : " + Math.floor(statistics.calculateTurnaround()*100)/100 + " Milliseconds");
        System.out.println("Average Waiting Time : " + Math.floor(statistics.calculateWait()*100)/100 + " Milliseconds");

    }




}
