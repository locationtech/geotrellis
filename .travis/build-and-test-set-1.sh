#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project doc-examples" compile \
  "project spark" test || { exit 1; }
./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project cassandra" test || { exit 1; }
