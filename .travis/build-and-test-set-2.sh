#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project vector" test \
  "project raster" test \
  "project slick" test \
  "project vectortile" test \
  "project spark-pipeline" test \
  "project spark-etl" test \
  "project geowave" compile test:compile \
  "project hbase" test \
  "project accumulo" test \
  "project s3" test \
  "project s3-testkit" test \
  "project geomesa" test || { exit 1; }
