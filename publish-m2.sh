#!/bin/bash

./sbt "project macros" publish-m2 "project vector" publish-m2 "project proj4" publish-m2 "project raster" publish-m2 "project engine" publish-m2 "project testkit" publish-m2 "project services" publish-m2 "project jetty" publish-m2 "project spark" publish-m2 "project gdal" publish-m2 "project geotools" publish-m2 "project slick" publish-m2
