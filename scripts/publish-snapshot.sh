#!/bin/bash

./sbt "project accumulo" publish \
      "project cassandra" publish \
      "project geotools" publish \
      "project macros" publish \
      "project proj4" publish \
      "project raster" publish \
      "project raster-testkit" publish \
      "project s3" publish \
<<<<<<< HEAD
=======
      "project accumulo" publish \
      "project cassandra" publish \
      "project hbase" publish \
>>>>>>> master
      "project shapefile" publish \
      "project spark" publish \
      "project spark-testkit" publish \
      "project util" publish \
      "project vector" publish \
      "project vector-testkit" publish \
      "project vectortile" publish
