#!/bin/bash

./sbt -J-Xmx2G "project feature-test" test "project raster-test" test "project geotools" compile "project benchmark" compile "project demo" compile "project feature-benchmark" compile "project dev" compile "project services" compile "project jetty" compile "project admin" compile "project slick" test:compile "project gdal" test:compile
