#!/bin/bash

wget https://s3.amazonaws.com/geotrellis-test/geowave-minicluster/miniAccumuloCluster-assembly-0.1.0.jar

java -cp miniAccumuloCluster-assembly-0.1.0.jar geotrellis.minicluster.Main
