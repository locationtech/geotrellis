#!/bin/bash

./sbt "project accumulo-store" createHeaders test:createHeaders \
      "project accumulo-spark" createHeaders test:createHeaders \
      "project cassandra-store" createHeaders test:createHeaders \
      "project cassandra-spark" createHeaders test:createHeaders \
      "project doc-examples" createHeaders test:createHeaders \
      "project geomesa" createHeaders test:createHeaders \
      "project geotools" createHeaders test:createHeaders \
      "project geowave" createHeaders test:createHeaders \
      "project hbase-store" createHeaders  test:createHeaders  \
      "project hbase-spark" createHeaders  test:createHeaders  \
      "project proj4" createHeaders test:createHeaders \
      "project raster" createHeaders test:createHeaders \
      "project raster-testkit" createHeaders test:createHeaders \
      "project s3" createHeaders test:createHeaders  \
      "project shapefile" createHeaders test:createHeaders \
      "project spark" createHeaders test:createHeaders  \
      "project spark-testkit" createHeaders test:createHeaders \
      "project util" createHeaders test:createHeaders \
      "project vector" createHeaders test:createHeaders \
      "project vectortile" createHeaders test:createHeaders
