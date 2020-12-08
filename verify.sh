#!/bin/bash

sh clean.sh
sh trace.sh > verify/trace
cd verify
java -jar verify.jar trace
cd ..
