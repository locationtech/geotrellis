#!/bin/bash
# Publish local for main scala version (2.10)
./sbt "project macros" publish-local && \
./sbt "project vector" publish-local && \
./sbt "project proj4" publish-local && \
./sbt "project raster" publish-local && \
#./sbt "project index" publish-local && \
#./sbt "project engine" publish-local && \
#./sbt "project testkit" publish-local && \
#./sbt "project services" publish-local && \
#./sbt "project jetty" publish-local && \
./sbt "project spark" publish-local #&& \
#./sbt "project spark-etl" publish-local && \
./sbt "project gdal" publish-local && \
#./sbt "project geotools" publish-local && \
#./sbt "project slick" publish-local
