#!/bin/bash

.circleci/unzip-rasters.sh

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project s3" test \
  "project s3-spark" test || { exit 1; }
