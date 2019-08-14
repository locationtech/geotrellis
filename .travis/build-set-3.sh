#!/bin/bash

# "project gdal-spark" test \
./sbt "++$TRAVIS_SCALA_VERSION" \
  "project spark" test \
  "project spark-pipeline" test || { exit 1; }
