#!/bin/bash

# from https://github.com/echeipesh/geotrellis/blob/feature/better-catalog-filtering/buildall.sh

./sbt -J-Xmx2G "project index" test 
./sbt -J-Xmx2G "project proj4" test 
./sbt -J-Xmx2G "project vector-test" test 
./sbt -J-Xmx2G "project raster-test" test 
./sbt -J-Xmx2G "project engine-test" test 
./sbt -J-Xmx2G "project slick" test:compile 
./sbt -J-Xmx2G "project gdal" test:compile
./sbt -J-Xmx4G "project spark" package "project spark" test 

./sbt -J-Xmx2G "project geotools" compile 
./sbt -J-Xmx2G "project benchmark" compile 
./sbt -J-Xmx2G "project demo" compile 
./sbt -J-Xmx2G "project dev" compile 
./sbt -J-Xmx2G "project services" compile 
./sbt -J-Xmx2G "project jetty" compile 
./sbt -J-Xmx2G "project admin" compile 
./sbt -J-Xmx2G "project vector-benchmark" compile


