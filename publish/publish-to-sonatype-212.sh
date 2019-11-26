#!/bin/bash
# Publish to sonatype for all supported scala version 2.12

 # there is no geomesa project
 ./sbt -212 "project macros" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project vector" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project proj4" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project raster" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project spark-pipeline" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project s3" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project s3-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project accumulo" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project accumulo-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project hbase" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project hbase-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project cassandra" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project cassandra-spark" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project geotools" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project shapefile" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project layer" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project store" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project util" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project vectortile" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project raster-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project vector-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project spark-testkit" publishSigned -no-colors -J-Drelease=sonatype \
   && ./sbt -212 "project gdal" publishSigned -no-colors -J-Drelease=sonatype
