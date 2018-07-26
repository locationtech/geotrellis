#!/bin/bash

if [ $RUN_SET = "1" ]; then
    echo "RUNNING SET 1";
    .travis/build-and-test-set-1.sh;
elif [ $RUN_SET = "2" ]; then
    echo "RUNNING SET 2";
    .travis/build-and-test-set-2.sh;
else
    echo "RUNNING SET 3";
    .travis/build-and-test-set-3.sh;
fi
