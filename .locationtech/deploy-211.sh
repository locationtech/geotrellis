#!/usr/bin/env bash

 set -e
 set -x

 ./sbt -211 "project macros" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project vector" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project proj4" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project raster" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project spark-pipeline" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project s3" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project s3-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project accumulo" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project accumulo-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project hbase" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project hbase-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project cassandra" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project cassandra-spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project geotools" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project shapefile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project layer" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project store" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project util" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project vectortile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project raster-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project vector-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project spark-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -211 "project gdal" publish -no-colors -J-Drelease=locationtech
