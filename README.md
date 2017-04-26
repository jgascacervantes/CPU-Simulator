# CPU-Simulator
a muiltithreaded CPU simulation that tests different scheduling algorithms.

the input file format is  
proc [cpu io cpu io ...]  
sleep [sleep time(ms)]  
stop   

  
example input file:  
proc  1 10 20 10 50 20 40 10  
proc  1 50 10 30 20 40  
sleep 50  
proc  2 20 50 20  
stop  
