#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project doc-examples" test:compile \
  "project spark" test:compile \
  "project cassandra" test:compile || { exit 1; }
