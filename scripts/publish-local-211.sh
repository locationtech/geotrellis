#!/bin/bash
# Publish local for Scala 2.11
./sbt -211 "project macros" publish-local && \
./sbt -211 "project vector" publish-local && \
./sbt -211 "project proj4" publish-local && \
./sbt -211 "project raster" publish-local && \
./sbt -211 "project spark" publish-local && \
./sbt -211 "project s3" publish-local && \
./sbt -211 "project accumulo" publish-local && \
./sbt -211 "project cassandra" publish-local && \
./sbt -211 "project hbase" publish-local && \
./sbt -211 "project spark-etl" publish-local && \
./sbt -211 "project shapefile" publish-local && \
./sbt -211 "project slick" publish-local && \
./sbt -211 "project util" publish-local && \
./sbt -211 "project raster-testkit" publish-local && \
./sbt -211 "project vector-testkit" publish-local && \
./sbt -211 "project spark-testkit" publish-local
