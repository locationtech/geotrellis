#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project raster" test \
  "project accumulo" test \
  "project s3" test || { exit 1; }
