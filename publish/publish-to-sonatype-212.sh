#!/bin/bash
# Publish to sonatype for all supported scala version 2.12

 ./sbt "project macros" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project vector" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project proj4" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project raster" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project spark-pipeline" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project s3" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project s3-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project accumulo" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project accumulo-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project hbase" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project hbase-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project cassandra" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project cassandra-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project geotools" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project shapefile" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project layer" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project store" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project util" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project vectortile" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project raster-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project vector-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project spark-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt "project gdal" publishSigned -no-colors -J-Drelease=sonatype
