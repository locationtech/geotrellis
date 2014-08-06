#!/bin/bash

./sbt "project macros" publish "project vector" publish "project proj4" publish "project raster" publish "project engine" publish "project testkit" publish "project services" publish "project jetty" publish "project spark" publish "project gdal" publish "project geotools" publish
