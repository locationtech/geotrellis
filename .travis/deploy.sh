#!/usr/bin/env bash

set -e
set -x

mkdir -p "${HOME}/.bintray"

cat <<EOF > "${HOME}/.bintray/.credentials"
realm = Bintray API Realm
host = api.bintray.com
user = $BINTRAY_USER
password = $BINTRAY_API_KEY
EOF

./sbt "++$TRAVIS_SCALA_VERSION" "project macros" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project vector" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project proj4" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project raster" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project engine" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project services" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project jetty" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project spark" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project spark-etl" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project gdal" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project shapefile" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project slick" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project util" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project raster-testkit" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project vector-testkit" publish \
  && ./sbt "++$TRAVIS_SCALA_VERSION" "project spark-testkit" publish
