#!/bin/bash

# ./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project geowave" compile test:compile || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project cassandra" test  || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project vector-test" test || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project raster-test" test || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project s3-test" test  || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project s3-testkit" test  || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project spark-etl" compile  || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project slick" test || { exit 1; }
./sbt -J-Xss256m -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project vectortile" test || { exit 1; }
