#!/bin/bash

./sbt "project accumulo" publish -J-Drelease=locationtech \
      "project accumulo-spark" publish -J-Drelease=locationtech \
      "project cassandra" publish -J-Drelease=locationtech \
      "project cassandra-spark" publish -J-Drelease=locationtech \
      "project geotools" publish -J-Drelease=locationtech \
      "project hbase" publish -J-Drelease=locationtech \
      "project macros" publish -J-Drelease=locationtech \
      "project proj4" publish -J-Drelease=locationtech \
      "project raster" publish -J-Drelease=locationtech \
      "project raster-testkit" publish -J-Drelease=locationtech \
      "project s3" publish -J-Drelease=locationtech \
      "project s3-spark" publish -J-Drelease=locationtech \
      "project hbase" publish -J-Drelease=locationtech \
      "project hbase-spark" publish -J-Drelease=locationtech \
      "project shapefile" publish -J-Drelease=locationtech \
      "project spark" publish -J-Drelease=locationtech \
      "project spark-testkit" publish -J-Drelease=locationtech \
      "project store" publish -J-Drelease=locationtech \
      "project util" publish -J-Drelease=locationtech \
      "project vector" publish -J-Drelease=locationtech \
      "project vector-testkit" publish -J-Drelease=locationtech \
      "project vectortile" publish -J-Drelease=locationtech \
      "project gdal" publish -J-Drelease=locationtech
