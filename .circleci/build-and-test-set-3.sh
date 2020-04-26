#!/bin/bash

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project spark" test \
  "project spark-pipeline" test && \
./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project gdal-spark" test || { exit 1; }
