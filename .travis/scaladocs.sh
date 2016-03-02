#!/bin/bash

set -o errexit -o nounset

if [ "$TRAVIS_BRANCH" != "master" ]
then
  echo "This commit was made against the $TRAVIS_BRANCH and not the master! No deploy!"
  exit 0
fi

rev=$(git rev-parse --short HEAD)

cd stage/_book

# Build docs
./sbt "++2.11.5 unidoc"

# Set up git
git config --global user.email "azaveadev@azavea.com"
git config --global user.name "azaveaci"

# Inside scaladocs from hereon
git clone https://github.com/geotrellis/scaladocs.git
rm -rf scaladocs/latest
mv target/scala-2.11/unidoc scaladocs/latest
cd scaladocs
git remote add originAuth https://$CI_GH_TOKEN@github.com/geotrellis/scaladocs.git

git add -A .
git commit -m "rebuild scaladocs at ${rev}"
git push -q originAuth HEAD:gh-pages
