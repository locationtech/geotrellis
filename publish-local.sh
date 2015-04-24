#!/bin/bash

./sbt "project macros" +publish-local "project vector" +publish-local "project proj4" +publish-local "project raster" +publish-local "project index" +publish-local "project engine" +publish-local "project testkit" +publish-local "project services" +publish-local "project jetty" +publish-local "project spark" +publish-local "project gdal" +publish-local "project geotools" +publish-local "project slick" +publish-local
