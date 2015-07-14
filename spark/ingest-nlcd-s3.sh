#!/bin/sh

export JAR=target/scala-2.10/geotrellis-spark-assembly-0.10.0-SNAPSHOT.jar

spark-submit \
--class geotrellis.spark.ingest.HadoopIngestCommand \
--master local[*] \
--driver-memory 8G \
$JAR \
--input file:///Volumes/Pod/tiles/nlcd_wm_ext-tiled-proj4 \
--layerName nlcd-tms --crs EPSG:3857 --clobber true \
--pyramid true

#--input file:///Users/eugene/tmp/nlcd_wm_ext-tiled \
#--input file:///Users/eugene/tmp/nlcd/subset
