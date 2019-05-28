#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project raster" test \
  "project accumulo-store" test \
  "project accumulo-spark" test \
  "project s3-store" test \
  "project s3-spark" test || { exit 1; }
