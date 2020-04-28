#!/bin/bash

.circleci/unzip-rasters.sh

if [ $RUN_SET = "1" ]; then
    echo "RUNNING SET 1";
    .circleci/build-and-test-set-1.sh;
elif [ $RUN_SET = "2" ]; then
    echo "RUNNING SET 2";
    .circleci/build-and-test-set-2.sh;
else
    echo "Skipping RUN_SET = $RUN_SET in build-and-test.sh."
fi
