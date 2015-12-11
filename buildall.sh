#!/bin/bash

./sbt -J-Xmx2G "project spark" test  || { exit 1; }
