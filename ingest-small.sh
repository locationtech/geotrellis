#!/bin/sh

JAR=spark/target/scala-2.10/geotrellis-spark-assembly-0.10.0-SNAPSHOT.jar

zip -d $JAR META-INF/ECLIPSEF.RSA
zip -d $JAR META-INF/ECLIPSEF.SF

AWS_ID=AKIAJLIB4YFFVMY4TT5A
AWS_KEY=ZcjWmdXN+7leTlbptjEqBnQTqxDY3ESZgeJxGsj8

spark-submit \
--driver-memory 8g --master "local[8]" \
--driver-library-path /usr/local/lib \
--class climate.ingest.NEXIngest \
$JAR \
--instance gis --zookeeper localhost \
--user root --password secret \
--input s3n://$AWS_ID:$AWS_KEY@nex-bcsd-tiled-geotiff/rcp26/pr/CCSM4/pr_amon_BCSD_rcp26_r1i1p1_CONUS_CCSM4_200601-201012-200601120000_0_0 \
--crs EPSG:3857 --layerName pr-rcp26-ccsm4-small --table pr_small --clobber true 