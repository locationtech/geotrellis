#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project doc-examples" compile || { exit 1; }
./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project spark" test || { exit 1; }
./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project accumulo" test "project cassandra" test || { exit 1; }
