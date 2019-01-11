#!/usr/bin/env bash

 set -e
 set -x

 # there is no geomesa and slick projects
 ./sbt -212 "project macros" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project vector" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project proj4" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project raster" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project spark" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project s3" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project accumulo" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project cassandra" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project hbase" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project spark-etl" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project geotools" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project shapefile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project util" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project vectortile" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project raster-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project vector-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project spark-testkit" publish -no-colors -J-Drelease=locationtech \
   && ./sbt -212 "project s3-testkit" publish -no-colors -J-Drelease=locationtech
