#!/bin/bash
# Publish to sonatype for all supported scala version 2.12

 ./sbt -213 publishSigned -no-colors -J-Drelease=sonatype
