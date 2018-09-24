#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project spark" compile test:compile \
  "project spark-pipeline" compile test:compile \
  "project spark-etl" compile test:compile || { exit 1; }
