#!/bin/bash

./sbt "++$TRAVIS_SCALA_VERSION" \
  "project raster" test || { exit 1; }
  # "project accumulo" test \
  # "project s3" test \
  # "project s3-testkit" test || { exit 1; }
