#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project layer" test \
  "project store" test \
  "project vector" test \
  "project vectortile" test \
  "project gdal" test \
  "project hbase" test \
  "project hbase-spark" test \
  "project cassandra" test \
  "project cassandra-spark" test || { exit 1; }
