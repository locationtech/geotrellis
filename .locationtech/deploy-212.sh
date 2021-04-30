#!/usr/bin/env bash

set -e
set -x

./sbt publish -no-colors -J-Drelease=locationtech
