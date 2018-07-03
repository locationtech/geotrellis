#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project doc-examples" compile \
  "project vector" test \
  "project slick" test \
  "project vectortile" test \
  "project geowave" compile test:compile \
  "project hbase" test \
  "project geomesa" test \
  "project cassandra" test || { exit 1; }
