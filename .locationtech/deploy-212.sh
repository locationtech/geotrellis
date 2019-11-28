#!/usr/bin/env bash

 set -e
 set -x

 ./sbt "project macros" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project vector" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project proj4" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project raster" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project spark-pipeline" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project s3" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project s3-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project accumulo" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project accumulo-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project hbase" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project hbase-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project cassandra" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project cassandra-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project geotools" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project shapefile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project layer" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project store" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project util" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project vectortile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project raster-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project vector-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project spark-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt "project gdal" publish -no-colors -J-Drelease=locationtech
