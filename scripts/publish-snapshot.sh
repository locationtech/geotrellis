#!/bin/bash

./sbt "project macros" publish \
      "project vector" publish \
      "project proj4" publish \
      "project raster" publish \
      "project engine" publish \
      "project spark" publish \
      "project s3" publish \
      "project accumulo" publish \
      "project gdal" publish \
      "project shapefile" publish \
      "project util" publish \
      "project raster-testkit" publish \
      "project vector-testkit" publish \
      "project spark-testkit" publish
