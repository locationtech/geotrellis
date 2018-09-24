#!/bin/bash

./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" \
  "project raster" test \
  "project accumulo" compile test:compile \
  "project s3" compile test:compile \
  "project s3-testkit" compile test:compile || { exit 1; }
