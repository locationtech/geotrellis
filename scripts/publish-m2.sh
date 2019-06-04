#!/bin/bash

./sbt "project accumulo-store" +publishM2 \
      "project accumulo-spark" +publishM2 \
      "project cassandra-store" +publishM2 \
      "project cassandra-spark" +publishM2 \
      "project geomesa" +publishM2 \
      "project geotools" +publishM2 \
      "project geowave" +publishM2 \
      "project hbase-store" +publishM2 \
      "project hbase-spark" +publishM2 \
      "project macros" +publishM2 \
      "project proj4" +publishM2 \
      "project raster" +publishM2 \
      "project raster-testkit" +publishM2 \
      "project s3" +publishM2 \
      "project shapefile" +publishM2 \
      "project spark-pipeline" publishM2 \
      "project spark" +publishM2 \
      "project spark-testkit" +publishM2 \
      "project util" +publishM2 \
      "project vector" +publishM2 \
      "project vector-testkit" +publishM2 \
      "project vectortile" +publishM2
