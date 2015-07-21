#!/bin/bash

./sbt -J-Xmx2G "project proj4" test || { exit 1; } 
./sbt -J-Xmx2G "project vector-test" test || { exit 1; }
./sbt -J-Xmx2G "project raster-test" test || { exit 1; } 
./sbt -J-Xmx2G "project spark" test  || { exit 1; }
./sbt -J-Xmx2G "project engine-test" test || { exit 1; } 
./sbt -J-Xmx2G "project index" test || { exit 1; }
./sbt -J-Xmx2G "project benchmark" compile || { exit 1; } 
./sbt -J-Xmx2G "project slick" test:compile || { exit 1; } 
./sbt -J-Xmx2G "project gdal" test:compile || { exit 1; }
./sbt -J-Xmx2G "project geotools" compile || { exit 1; } 
./sbt -J-Xmx2G "project demo" compile || { exit 1; } 
./sbt -J-Xmx2G "project dev" compile || { exit 1; } 
./sbt -J-Xmx2G "project services" compile || { exit 1; } 
./sbt -J-Xmx2G "project jetty" compile || { exit 1; } 
./sbt -J-Xmx2G "project admin" compile || { exit 1; } 
./sbt -J-Xmx2G "project vector-benchmark" compile || { exit 1; } 
