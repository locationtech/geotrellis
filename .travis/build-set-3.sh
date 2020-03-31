#!/bin/bash

./sbt -Dsbt.supershell=false "++$TRAVIS_SCALA_VERSION" \
  "project spark" test \
  "project gdal-spark" test \
  "project spark-pipeline" test || { exit 1; }
