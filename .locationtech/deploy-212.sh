#!/usr/bin/env bash

set -e
set -x

./sbt -213 publish -no-colors -J-Drelease=locationtech
