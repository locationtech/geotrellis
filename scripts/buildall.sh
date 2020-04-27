#!/bin/bash

./sbt \
  "project accumulo" test \
  "project accumulo-spark" test \
  "project cassandra" test \
  "project cassandra-spark" test \
  "project gdal" test \
  "project gdal-spark" test \
  "project geotools" test \
  "project hbase" test \
  "project hbase-spark" test \
  "project layer" test \
  "project proj4" test \
  "project raster" test \
  "project s3" test \
  "project s3-spark" test \
  "project shapefile" test \
  "project spark" test \
  "project spark-pipeline" test \
  "project store" test \
  "project util" test \
  "project vector" test \
  "project vectortile" test || { exit 1; }
