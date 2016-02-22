#!/bin/bash

./sbt -J-Xmx2G "project proj4" clean || { exit 1; }
./sbt -J-Xmx2G "project vector-test" clean || { exit 1; }
./sbt -J-Xmx2G "project raster-test" clean || { exit 1; }
./sbt -J-Xmx2G "project spark" clean  || { exit 1; }
./sbt -J-Xmx2G "project engine-test" clean || { exit 1; }
./sbt -J-Xmx2G "project benchmark" clean || { exit 1; }
./sbt -J-Xmx2G "project slick" clean || { exit 1; }
./sbt -J-Xmx2G "project gdal" clean || { exit 1; }
./sbt -J-Xmx2G "project shapefile" clean || { exit 1; }
./sbt -J-Xmx2G "project vector-benchmark" clean || { exit 1; }
./sbt -J-Xmx2G "project util" clean || { exit 1; }

rm -r proj4/target
rm -r macros/target
rm -r vector/target
rm -r vector-test/target
rm -r raster/target
rm -r raster-test/target
rm -r spark/target
rm -r engine/target
rm -r engine-test/target
rm -r benchmark/target
rm -r slick/target
rm -r gdal/target
rm -r shapefile/target
rm -r vector-benchmark/target
rm -r tasks/target
rm -r util/target
rm -r raster-testkit/target
rm -r vector-testkit/target
rm -r spark-testkit/target
