#!/bin/bash

if [ $RUN_SET = "1" ]; then
    echo "RUNNING SET 1";
    if [ `echo $TRAVIS_SCALA_VERSION | cut -f1-2 -d "."` = "2.11" ]; then
        .travis/build-and-test-set-1.sh;
    else
        .travis/build-set-1.sh;
    fi
elif [ $RUN_SET = "2" ]; then
    echo "RUNNING SET 2";
    if [ `echo $TRAVIS_SCALA_VERSION | cut -f1-2 -d "."` = "2.11" ]; then
        .travis/build-and-test-set-2.sh;
    else
        .travis/build-set-2.sh;
    fi
else
    echo "RUNNING SET 3";
    if [ `echo $TRAVIS_SCALA_VERSION | cut -f1-2 -d "."` = "2.11" ]; then
        .travis/build-and-test-set-3.sh;
    else
        .travis/build-set-3.sh;
    fi
fi
