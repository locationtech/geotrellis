#!/bin/bash

set -o errexit -o nounset

if [ "$CIRCLE_BRANCH" != "master" ] || [ -z ${CI_GH_TOKEN:-} ]
then
  echo "This commit was made against the $CIRCLE_BRANCH, not the master or the CI_GH_TOKEN is not set! No deploy!"
  exit 0
fi

DIR="$(dirname "$0")/.."
SCALADOCS_CHECKOUT_DIR="${1:-/tmp}/scaladocs"
SCALADOCS_REPO="https://github.com/geotrellis/scaladocs.git"
SCALADOCS_BRANCH="gh-pages"

pushd "$DIR"
rev=$(git rev-parse --short HEAD)

# Set up git
git config --global user.email "azaveadev@azavea.com"
git config --global user.name "azaveaci"

rm -rf "$SCALADOCS_CHECKOUT_DIR"
git clone "$SCALADOCS_REPO" "$SCALADOCS_CHECKOUT_DIR"
rm -rf "$SCALADOCS_CHECKOUT_DIR/latest"
mv "target/scala-2.12/unidoc" "$SCALADOCS_CHECKOUT_DIR/latest"

pushd "$SCALADOCS_CHECKOUT_DIR"
git remote add originAuth https://$CI_GH_TOKEN@github.com/geotrellis/scaladocs.git

echo "Pushing scaladoc to $SCALADOCS_REPO#$SCALADOCS_BRANCH"
git add -A .
git commit -m "rebuild scaladocs at ${rev}"
git push -q originAuth "$SCALADOCS_BRANCH"
