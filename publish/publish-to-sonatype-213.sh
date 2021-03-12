#!/usr/bin/env bash

# Publish to sonatype for all supported scala version 2.13

set -Eeuo pipefail
set -x

./sbt -213 publishSigned -no-colors -J-Drelease=sonatype
