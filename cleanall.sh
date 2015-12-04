#!/bin/bash

./sbt -J-Xmx2G "project proj4" clean || { exit 1; } 
./sbt -J-Xmx2G "project vector-test" clean || { exit 1; }
./sbt -J-Xmx2G "project raster-test" clean || { exit 1; } 
./sbt -J-Xmx2G "project spark" clean  || { exit 1; }
./sbt -J-Xmx2G "project engine-test" clean || { exit 1; } 
./sbt -J-Xmx2G "project index" clean || { exit 1; }
./sbt -J-Xmx2G "project benchmark" clean || { exit 1; } 
./sbt -J-Xmx2G "project slick" clean || { exit 1; } 
./sbt -J-Xmx2G "project gdal" clean || { exit 1; }
./sbt -J-Xmx2G "project geotools" clean || { exit 1; } 
./sbt -J-Xmx2G "project demo" clean || { exit 1; } 
./sbt -J-Xmx2G "project dev" clean || { exit 1; } 
./sbt -J-Xmx2G "project services" clean || { exit 1; } 
./sbt -J-Xmx2G "project jetty" clean || { exit 1; } 
./sbt -J-Xmx2G "project admin" clean || { exit 1; } 
./sbt -J-Xmx2G "project vector-benchmark" clean || { exit 1; } 
