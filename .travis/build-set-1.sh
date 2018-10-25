#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project vector" test \
  "project vectortile" test || { exit 1; }
  # "project slick" test \
  # "project geowave" compile test:compile \
  # "project hbase" test \
  # "project geomesa" test \
  # "project cassandra" test || { exit 1; }
