#!/bin/sh

## compile Trace.java and put .class into bin
javac ticketingsystem/Trace.java

result=1

## begin test
for i in $(seq 1 50); do ## you can change the number of test, default is 50
    java -cp . ticketingsystem/Trace  > trace 
    java -jar checker.jar --coach 3 --seat 5 --station 5 < trace
    if [ $? != 0 ]; then
        echo "Test failed!!! see trace file to debug"
        result=0
        break
    fi
done

if [ $result == 1 ]; then
    echo "Test passed!!!"
fi
