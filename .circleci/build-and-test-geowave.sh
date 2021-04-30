#!/bin/bash

.circleci/unzip-rasters.sh

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project geowave" test || { exit 1; }
