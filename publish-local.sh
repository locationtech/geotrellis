#!/bin/bash

./sbt "project macros" publish-local && ./sbt "project feature" publish-local && ./sbt "project proj4" publish-local && ./sbt "project core" publish-local && ./sbt "project testkit" publish-local && ./sbt "project services" publish-local && ./sbt "project jetty" publish-local && ./sbt "project spark" publish-local && ./sbt "project gdal" publish-local && ./sbt "project geotools" publish-local
