#!/bin/bash

./sbt -J-Xmx2G "project accumulo" test  || { exit 1; }
./sbt -J-Xmx2G "project layers-accumulo" test  || { exit 1; }
./sbt -J-Xmx2G "project cassandra" test  || { exit 1; }
./sbt -J-Xmx2G "project doc-examples" compile || { exit 1; }
./sbt -J-Xmx2G "project geomesa" test || { exit 1; }
./sbt -J-Xmx2G "project geotools" test || { exit 1; }
HOSTALIASES=/tmp/hostaliases ./sbt -J-Xmx2G "project geowave" test || { exit 1; }
./sbt -J-Xmx2G "project hbase" test  || { exit 1; }
./sbt -J-Xmx2G "project proj4" test || { exit 1; }
./sbt -J-Xmx2G "project raster" test || { exit 1; }
./sbt -J-Xmx2G "project raster-testkit" compile || { exit 1; }
./sbt -J-Xmx2G "project s3" test  || { exit 1; }
./sbt -J-Xmx2G "project shapefile" compile || { exit 1; }
./sbt -J-Xmx2G "project spark" test  || { exit 1; }
./sbt -J-Xmx2G "project spark-etl" compile  || { exit 1; }
./sbt -J-Xmx2G "project spark-testkit" compile || { exit 1; }
./sbt -J-Xmx2G "project util" compile || { exit 1; }
./sbt -J-Xmx2G "project vector" test || { exit 1; }
./sbt -J-Xmx2G "project vectortile" test || { exit 1; }
