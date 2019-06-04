#!/bin/bash

# Publish local for main scala version (2.11)
# Ordered roughly by dependency graph, which is easier on the compilation process

./sbt "project util" publishLocal && \
./sbt "project macros" publishLocal && \
./sbt "project proj4" publishLocal && \
./sbt "project vector" publishLocal && \
./sbt "project vector-testkit" publishLocal && \
./sbt "project raster" publishLocal && \
./sbt "project raster-testkit" publishLocal && \
./sbt "project vectortile" publishLocal && \
./sbt "project spark" publishLocal && \
./sbt "project spark-testkit" publishLocal && \
./sbt "project shapefile" publishLocal && \
./sbt "project spark-pipeline" publishLocal && \
./sbt "project accumulo-store" publishLocal && \
./sbt "project accumulo-spark" publishLocal && \
./sbt "project cassandra-store" publishLocal && \
./sbt "project cassandra-spark" publishLocal && \
./sbt "project geomesa" publishLocal && \
./sbt "project geotools" publishLocal && \
./sbt "project geowave" publishLocal && \
./sbt "project hbase-store" publishLocal && \
./sbt "project hbase-spark" publishLocal && \
./sbt "project s3" publishLocal
