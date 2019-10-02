#!/bin/bash

set -o errexit -o nounset

if [ "$TRAVIS_BRANCH" != "master" ]
then
  echo "This commit was made against the $TRAVIS_BRANCH and not the master! No deploy!"
  exit 0
fi

rev=$(git rev-parse --short HEAD)

echo "Building scaladoc for Scala $TRAVIS_SCALA_VERSION at $rev"

# Build docs
./sbt "++$TRAVIS_SCALA_VERSION" unidoc

# Set up git
git config --global user.email "azaveadev@azavea.com"
git config --global user.name "azaveaci"

# Inside scaladocs from hereon
SCALADOC_REPO="https://github.com/geotrellis/scaladocs.git"
SCALADOC_BRANCH="gh-pages"
rm -rf scaladocs
git clone "$SCALADOC_REPO"
rm -rf scaladocs/latest
mv target/scala-2.11/unidoc scaladocs/latest
cd scaladocs
git remote add originAuth https://$CI_GH_TOKEN@github.com/geotrellis/scaladocs.git

echo "Pushing scaladoc to $SCALADOC_REPO#$SCALADOC_BRANCH"
git add -A .
git commit -m "rebuild scaladocs at ${rev}"
exit 0
git push -q originAuth "$SCALADOC_BRANCH"
