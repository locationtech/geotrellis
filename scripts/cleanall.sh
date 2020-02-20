#!/bin/bash

./sbt -J-Xmx2G "project accumulo" clean  || { exit 1; }
./sbt -J-Xmx2G "project accumulo-spark" clean  || { exit 1; }
./sbt -J-Xmx2G "project cassandra" clean  || { exit 1; }
./sbt -J-Xmx2G "project geotools" clean || { exit 1; }
./sbt -J-Xmx2G "project hbase" clean || { exit 1; }
./sbt -J-Xmx2G "project hbase-spark" clean || { exit 1; }
./sbt -J-Xmx2G "project proj4" clean || { exit 1; }
./sbt -J-Xmx2G "project s3" clean || { exit 1; }
./sbt -J-Xmx2G "project shapefile" clean || { exit 1; }
./sbt -J-Xmx2G "project spark" clean  || { exit 1; }
./sbt -J-Xmx2G "project spark-spark" clean  || { exit 1; }
./sbt -J-Xmx2G "project util" clean || { exit 1; }
./sbt -J-Xmx2G "project vector" clean || { exit 1; }
./sbt -J-Xmx2G "project vectortile" clean || { exit 1; }

rm -r accumulo/target
rm -r accumulo-spark/target
rm -r cassandra/target
rm -r cassandra-spark/target
rm -r geotools/target
rm -r hbase/target
rm -r hbase-spark/target
rm -r macros/target
rm -r proj4/target
rm -r raster/target
rm -r raster-testkit/target
rm -r s3/target
rm -r s3-spark/target
rm -r shapefile/target
rm -r spark-testkit/target
rm -r spark/target
rm -r util/target
rm -r vector-testkit/target
rm -r vector/target
rm -r vectortile/target
