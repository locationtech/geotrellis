#!/bin/bash

./sbt "project macros" publish \
      "project vector" publish \
      "project proj4" publish \
      "project raster" publish \
      "project geotools" publish \
      "project spark" publish \
      "project s3" publish \
      "project accumulo" publish \
      "project cassandra" publish \
      "project hbase" publish \
      "project shapefile" publish \
      "project util" publish \
      "project raster-testkit" publish \
      "project vector-testkit" publish \
      "project spark-testkit" publish
