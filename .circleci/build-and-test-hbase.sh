#!/bin/bash

.circleci/unzip-rasters.sh

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project hbase" test \
  "project hbase-spark" test || { exit 1; }
