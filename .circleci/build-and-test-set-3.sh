#!/bin/bash

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project spark" test \
  "project gdal-spark" test:compile \
  "project spark-pipeline" test || { exit 1; }
