#!/bin/bash

projects=("index" "proj4" "vector-test" "raster-test" "geotools" "benchmark" "demo" "vector-benchmark" "engine-test" "spark" "dev" "services" "jetty" "admin" "slick" "gdal")

for project in projects; do
  ./sbt -J-Xmx2G "project $project" test 
done