#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project tiling" test \
  "project doc-examples" compile \
  "project vector" test \
  "project vectortile" test \
  "project geowave" compile test:compile \
  "project hbase-store" compile \
  "project hbase-spark" test \
  "project geomesa" compile test:compile \
  "project cassandra-store" compile \
  "project cassandra-spark" test || { exit 1; }
