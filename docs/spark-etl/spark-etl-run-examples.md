# Spark ETL run commands examples

Standard ETL assembly provides two classes to ingest objects: class to ingest singleband tiles and class to ingest multiband tiles.
Class name to ingest singleband tiles is `geotrellis.spark.etl.SinglebandIngest` and to ingest multiband tiles is `geotrellis.spark.etl.MultibandIngest`.

### Ingest tiles from local fs or hdfs into s3 storage command

```sh
#!/bin/sh
export JAR="geotrellis-etl-assembly-0.10-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.{SinglebandIngest | MultibandIngest} \
--master local[*] \
--driver-memory 2G \
$JAR \
--input hadoop --format {geotiff | temporal-geotiff} --cache NONE -I path="file:///Data/nlcd/tiles" \
--output s3 -O bucket=com.azavea.datahub key=catalog \
--layer nlcd-tms --crs EPSG:3857 --pyramid --layoutScheme {tms | floating}
```

### Ingest singleband tiles from local fs or hdfs into accumulo storage command

```sh
#!/bin/sh
export JAR="geotrellis-etl-assembly-0.10-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.{SinglebandIngest | MultibandIngest} \
--master local[*] \
--driver-memory 2G \
$JAR \
--input hadoop --format {geotiff | temporal-geotiff} --cache NONE -I path="file:///Data/nlcd/tiles" \
--output accumulo -O instance="gis-accumulo-instance" table="nlcd-table" user="root" password="password" zookeeper="zoo1:2181,zoo2:2181,zoo3:2181" \
--layer nlcd-tms --crs EPSG:3857 --pyramid --layoutScheme {tms | floating}
```

### Ingest singleband tiles from local fs or hdfs into hadoop storage

```sh
#!/bin/sh
export JAR="geotrellis-etl-assembly-0.10-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.{SinglebandIngest | MultibandIngest} \
--master local[*] \
--driver-memory 2G \
$JAR \
--input hadoop  --format {geotiff | temporal-geotiff} --cache NONE -I path="file:///Data/nlcd/tiles" \
--output hadoop -O path="hdfs://geotrellis-ingest/nlcd/" \
--layer nlcd-tms --crs EPSG:3857 --pyramid --layoutScheme {tms | floating}
```

### Ingest singleband tiles from local fs or hdfs into rendered set of PNGs in S3

```sh
#!/bin/sh
export JAR="geotrellis-etl-assembly-0.10-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.{SinglebandIngest | MultibandIngest} \
--master local[*] \
--driver-memory 2G \
$JAR \
--input hadoop  --format {geotiff | temporal-geotiff} --cache NONE -I path="file:///Data/nlcd/tiles" \
--output render -O encoding={png | geotiff} path=s3://tms-bucket/layers/{name}/{z}-{x}-{y}.png \

### Ingest singleband tiles from local fs or hdfs into rendered set of PNGs in hdfs or local fs

```sh
#!/bin/sh
export JAR="geotrellis-etl-assembly-0.10-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.{SinglebandIngest | MultibandIngest} \
--master local[*] \
--driver-memory 2G \
$JAR \
--input hadoop  --format {geotiff | temporal-geotiff} --cache NONE -I path="file:///Data/nlcd/tiles" \
--output render -O encoding={png | geotiff} path=hdfs://tms-bucket/layers/{name}/{z}-{x}-{y}.png \
