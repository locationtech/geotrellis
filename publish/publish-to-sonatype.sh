#!/bin/bash
# Publish to sonatype for all supported scala version 2.11

./sbt "project macros" publish-signed \
    && ./sbt "project vector" publish-signed \
    && ./sbt "project proj4" publish-signed \
    && ./sbt "project raster" publish-signed \
    && ./sbt "project spark" publish-signed \
    && ./sbt "project s3" publish-signed \
    && ./sbt "project accumulo" publish-signed \
    && ./sbt "project cassandra" publish-signed \
    && ./sbt "project hbase" publish-signed \
    && ./sbt "project geomesa" publish-signed \
    && ./sbt "project geowave" publish-signed \
    && ./sbt "project geotools" publish-signed \
    && ./sbt "project shapefile" publish-signed \
    && ./sbt "project util" publish-signed \
    && ./sbt "project vectortile" publish-signed \
    && ./sbt "project raster-testkit" publish-signed \
    && ./sbt "project vector-testkit" publish-signed \
    && ./sbt "project spark-testkit" publish-signed
