#!/usr/bin/env bash

 set -e
 set -x

 ./sbt "project macros" publish \
   && ./sbt "project vector" publish \
   && ./sbt "project proj4" publish \
   && ./sbt "project raster" publish \
   && ./sbt "project spark" publish \
   && ./sbt "project s3" publish \
   && ./sbt "project accumulo" publish \
   && ./sbt "project cassandra" publish \
   && ./sbt "project hbase" publish \
   && ./sbt "project spark-etl" publish \
   && ./sbt "project geomesa" publish \
   && ./sbt "project geotools" publish \
   && ./sbt "project geowave" publish \
   && ./sbt "project shapefile" publish \
   && ./sbt "project slick" publish \
   && ./sbt "project util" publish \
   && ./sbt "project vectortile" publish \
   && ./sbt "project raster-testkit" publish \
   && ./sbt "project vector-testkit" publish \
   && ./sbt "project spark-testkit" publish
