#!/bin/bash

./sbt "project accumulo" +publishM2 \
      "project layers-accumulo" +publishM2 \
      "project layers-cassandra" +publishM2 \
      "project cassandra" +publishM2 \
      "project geomesa" +publishM2 \
      "project geotools" +publishM2 \
      "project geowave" +publishM2 \
      "project hbase" +publishM2 \
      "project macros" +publishM2 \
      "project proj4" +publishM2 \
      "project raster" +publishM2 \
      "project raster-testkit" +publishM2 \
      "project s3" +publishM2 \
      "project shapefile" +publishM2 \
      "project spark-pipeline" publishM2 \
      "project spark" +publishM2 \
      "project spark-etl" +publishM2 \
      "project spark-testkit" +publishM2 \
      "project util" +publishM2 \
      "project vector" +publishM2 \
      "project vector-testkit" +publishM2 \
      "project vectortile" +publishM2
