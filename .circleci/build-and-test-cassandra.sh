#!/bin/bash

.circleci/unzip-rasters.sh

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project cassandra" test \
  "project cassandra-spark" test || { exit 1; }
