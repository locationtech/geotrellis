#!/bin/bash

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project raster" test \
  "project accumulo" test \
  "project accumulo-spark" || { exit 1; }
