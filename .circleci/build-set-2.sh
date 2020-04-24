#!/bin/bash

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project raster" test \
  "project accumulo" test \
  "project accumulo-spark" test \
  "project s3" test \
  "project s3-spark" test || { exit 1; }
