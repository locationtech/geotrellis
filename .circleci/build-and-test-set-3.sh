#!/bin/bash

./sbt -Dsbt.supershell=false "++$SCALA_VERSION" \
  "project spark" test \
  "project spark-pipeline" test || { exit 1; }
