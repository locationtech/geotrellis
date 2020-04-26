#!/bin/bash

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project spark" test \
  "project spark-pipeline" test \
  "project gdal-spark" test || { exit 1; }
