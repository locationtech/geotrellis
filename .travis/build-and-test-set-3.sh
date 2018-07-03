#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project spark" test \
  "project spark-pipeline" test \
  "project spark-etl" test || { exit 1; }
