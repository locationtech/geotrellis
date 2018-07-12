#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project vector" test \
  "project raster" test \
  "project slick" test:compile \
  "project vectortile" test \
  "project spark-pipeline" test:compile \
  "project spark-etl" test:compile \
  "project hbase" test:compile \
  "project accumulo" test:compile \
  "project s3" test:compile \
  "project s3-testkit" test:compile || { exit 1; }
  # "project geowave" test:compile \
  # "project geomesa" test:compile || { exit 1; }
