#!/usr/bin/env bash

 set -e
 set -x

 ./sbt -213 "project macros" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project vector" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project proj4" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project raster" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project spark-pipeline" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project s3" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project s3-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project accumulo" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project accumulo-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project hbase" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project hbase-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project cassandra" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project cassandra-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project geotools" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project shapefile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project layer" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project store" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project util" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project vectortile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project raster-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project vector-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project spark-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -213 "project gdal" publish -no-colors -J-Drelease=locationtech
