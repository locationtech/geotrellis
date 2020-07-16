#!/bin/bash
# Publish to sonatype for all supported scala version 2.11

./sbt -211 publishSigned -no-colors -J-Drelease=sonatype
