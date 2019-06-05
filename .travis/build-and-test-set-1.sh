#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project doc-examples" compile \
  "project vector" test \
  "project vectortile" test \
  "project geowave" compile test:compile \
  "project hbase-store" compile \
  "project hbase-spark" test \
  "project geomesa" compile test:compile \
  "project cassandra" test || { exit 1; }
