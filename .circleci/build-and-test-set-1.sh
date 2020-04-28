#!/bin/bash

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project proj4" test \
  "project geotools" test \
  "project shapefile" test \
  "project layer" test \
  "project store" test \
  "project doc-examples" compile \
  "project vector" test \
  "project vectortile" test \
  "project util" test \
  "project raster" test \
  "project accumulo" test \
  "project accumulo-spark" test && \
./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project gdal" test || { exit 1; }
