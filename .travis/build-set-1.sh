#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project tiling" test \
  "project vector" test \
  "project vectortile" test \
  "project hbase-store" test \
  "project hbase-spark" test \
  "project cassandra-store" test \
  "project cassandra-spark" test || { exit 1; }
  # "project geomesa" test
  # "project geowave" compile test:compile
