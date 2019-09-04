#!/usr/bin/env bash

docker run -it --net=host \
  -v $HOME/.ivy2:/root/.ivy2 \
  -v $HOME/.sbt:/root/.sbt \
  -v $TRAVIS_BUILD_DIR:/geotrellis \
  -e RUN_SET=$RUN_SET \
  -e TRAVIS_SCALA_VERSION=$TRAVIS_SCALA_VERSION \
  -e TRAVIS_COMMIT=$TRAVIS_COMMIT \
  -e TRAVIS_JDK_VERSION=$TRAVIS_JDK_VERSION quay.io/azavea/openjdk-gdal:2.4-jdk8-slim /bin/bash -c "cd /geotrellis; .travis/build-and-test.sh"