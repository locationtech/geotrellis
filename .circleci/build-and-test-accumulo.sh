#!/bin/bash

.circleci/unzip-rasters.sh

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project accumulo" test \
  "project accumulo-spark" test || { exit 1; }
