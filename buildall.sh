#!/bin/bash

./sbt -J-Xmx2G test && ./sbt -J-Xmx2G "project demo" compile && ./sbt -J-Xmx2G "project feature" test && ./sbt -J-Xmx2G "project geotools" compile && ./sbt -J-Xmx2G "project tasks" compile && ./sbt -J-Xmx2G "project benchmark" compile && ./sbt -J-Xmx2G "project feature-benchmark" compile && ./sbt -J-Xmx2G "project dev" compile && ./sbt -J-Xmx2G "project services" compile && ./sbt -J-Xmx2G "project jetty" compile && ./sbt -J-Xmx2G "project admin" compile
