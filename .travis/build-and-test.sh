#!/bin/bash

COMMIT_HASH_NUMBER=`printf %i "'${TRAVIS_COMMIT: -1}"`
SCALA_MINOR_VERSION_NUMBER=`echo $TRAVIS_SCALA_VERSION | cut -d . -f2`
JAVA_VERSION_NUMBER=`echo ${TRAVIS_JDK_VERSION: -1}`
JDK_VERSION_NUMBER=`printf %i "'${TRAVIS_JDK_VERSION:3:3}"` # The 3rd letter in "openjdk" or "oracle", translates to 110 or 99

# Since the whole test suite runs too long for travis,
# we run a subset of the tests based on the mod 2 of
# a number derived from the hash, the scala version and the java jdk version.

RUN_SET=$((($COMMIT_HASH_NUMBER + $SCALA_MINOR_VERSION_NUMBER + $JAVA_VERSION_NUMBER + $JDK_VERSION_NUMBER) % 2))

echo "USING SCALA VERSION: $TRAVIS_SCALA_VERSION";
echo "USING JDK VERSION: $TRAVIS_JDK_VERSION";
echo "RUN SET CALC: ($COMMIT_HASH_NUMBER + $SCALA_MINOR_VERSION_NUMBER + $JAVA_VERSION_NUMBER + $JDK_VERSION_NUMBER) % 2 == $RUN_SET";

if [ $RUN_SET = "1" ]; then
    echo "RUNNING SET 1";
    .travis/build-and-test-set-1.sh;
else
    echo "RUNNING SET 2";
    .travis/build-and-test-set-2.sh;
fi
