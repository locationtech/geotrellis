#!/bin/bash

./sbt -J-Xmx2G ++2.11.8 "project proj4" test || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project vector-test" test || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project raster-test" test || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project spark" test  || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project s3" test  || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project accumulo" test  || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project cassandra" test  || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project hbase" test  || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project spark-etl" compile  || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project geotools" test || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project slick" test:compile || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project shapefile" compile || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project util" compile || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project doc-examples" compile || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project raster-testkit" compile || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project vector-testkit" compile || { exit 1; }
./sbt -J-Xmx2G ++2.11.8 "project spark-testkit" compile || { exit 1; }
