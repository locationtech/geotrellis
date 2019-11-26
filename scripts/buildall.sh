#!/bin/bash

./sbt -J-Xmx2G "project accumulo" test  || { exit 1; }
./sbt -J-Xmx2G "project accumulo-spak" test  || { exit 1; }
./sbt -J-Xmx2G "project cassandra" test  || { exit 1; }
./sbt -J-Xmx2G "project cassandra-spark" test  || { exit 1; }
./sbt -J-Xmx2G "project doc-examples" compile || { exit 1; }
./sbt -J-Xmx2G "project geotools" test || { exit 1; }
./sbt -J-Xmx2G "project hbase" test  || { exit 1; }
./sbt -J-Xmx2G "project hbase-spark" test  || { exit 1; }
./sbt -J-Xmx2G "project proj4" test || { exit 1; }
./sbt -J-Xmx2G "project raster" test || { exit 1; }
./sbt -J-Xmx2G "project raster-testkit" compile || { exit 1; }
./sbt -J-Xmx2G "project s3" test  || { exit 1; }
./sbt -J-Xmx2G "project s3-spark" test  || { exit 1; }
./sbt -J-Xmx2G "project shapefile" compile || { exit 1; }
./sbt -J-Xmx2G "project spark" test  || { exit 1; }
./sbt -J-Xmx2G "project spark-testkit" compile || { exit 1; }
./sbt -J-Xmx2G "project layer" compile || { exit 1; }
./sbt -J-Xmx2G "project store" compile || { exit 1; }
./sbt -J-Xmx2G "project util" compile || { exit 1; }
./sbt -J-Xmx2G "project vector" test || { exit 1; }
./sbt -J-Xmx2G "project vectortile" test || { exit 1; }
