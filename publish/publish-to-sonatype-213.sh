#!/bin/bash
# Publish to sonatype for all supported scala version 2.13

 ./sbt -213 publishSigned -no-colors -J-Drelease=sonatype
