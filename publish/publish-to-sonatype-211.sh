#!/bin/bash
# Publish to sonatype for all supported scala version 2.11

./sbt -211 "project macros" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project vector" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project proj4" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project raster" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project spark-pipeline" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project s3" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project s3-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project accumulo" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project accumulo-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project hbase" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project hbase-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project cassandra" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project cassandra-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project geotools" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project shapefile" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project layer" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project store" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project util" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project vectortile" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project raster-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project vector-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project spark-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -211 "project gdal" publishSigned -no-colors -J-Drelease=sonatype
