#!/bin/bash
# Publish to sonatype for all supported scala version 2.12

 ./sbt publishSigned -no-colors -J-Drelease=sonatype
