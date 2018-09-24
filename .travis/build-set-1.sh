#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project doc-examples" compile \
  "project vector" test \
  "project slick" compile test:compile \
  "project vectortile" test \
  "project hbase" compile test:compile \
  "project cassandra" compile test:compile || { exit 1; }
