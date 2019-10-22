#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project layer" test \
  "project doc-examples" compile \
  "project vector" test \
  "project vectortile" test \
  "project gdal" test \
  "project util" test \
  "project geowave" compile test:compile \
  "project hbase" compile \
  "project hbase-spark" test \
  "project geomesa" compile test:compile \
  "project cassandra" compile \
  "project cassandra-spark" test || { exit 1; }
