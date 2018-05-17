#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project spark" test \
  "project accumulo" test \
  "project cassandra" test \
  "project doc-examples" compile || { exit 1; }
