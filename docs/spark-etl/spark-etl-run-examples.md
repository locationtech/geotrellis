# Spark ETL run commands examples

Standard ETL assembly provides two classes to ingest objects: class to ingest singleband tiles and class to ingest multiband tiles.
Class name to ingest singleband tiles is `geotrellis.spark.etl.SinglebandIngest` and to ingest multiband tiles is `geotrellis.spark.etl.MultibandIngest`.

Every example can be launched using:

```sh
#!/bin/sh
export JAR="geotrellis-etl-assembly-0.10-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.{SinglebandIngest | MultibandIngest} \
--master local[*] \
--driver-memory 2G \
$JAR \
--credentials "file://credentials.json" \
--datasets "file://datasets.json"
```

### Backend profiles JSON example

`credentials.json`:

```json
{  
   "backend-profiles":[  
      {  
         "name":"accumulo-name",
         "type":"accumulo",
         "zookeepers":"zookeepers",
         "instance":"instance",
         "user":"user",
         "password":"password"
      },
      {  
         "name":"cassandra-name",
         "type":"cassandra",
         "allowRemoteDCsForLocalConsistencyLevel":false,
         "localDc":"datacenter1",
         "usedHostsPerRemoteDc":0,
         "hosts":"hosts",
         "replicationStrategy":"SimpleStrategy",
         "replicationFactor":1,
         "user":"user",
         "password":"password"
      }
   ]
}
```

### Output JSON example

`output.json`:

```json
{  
   "backend":{  
      "type":"accumulo",
      "path":"output",
      "profile":"accumulo-name"
   },
   "breaks":"0:ffffe5ff;0.1:f7fcb9ff;0.2:d9f0a3ff;0.3:addd8eff;0.4:78c679ff;0.5:41ab5dff;0.6:238443ff;0.7:006837ff;1:004529ff",
   "reprojectMethod":"buffered",
   "cellSize":{  
      "width":256.0,
      "height":256.0
   },
   "encoding":"geotiff",
   "tileSize":256,
   "layoutExtent":{  
      "xmin":1.0,
      "ymin":2.0,
      "xmax":3.0,
      "ymax":4.0
   },
   "resolutionThreshold":0.1,
   "pyramid":true,
   "resampleMethod":"nearest-neighbor",
   "keyIndexMethod":{  
      "type":"zorder"
   },
   "layoutScheme":"zoomed",
   "cellType":"int8",
   "crs":"EPSG:3857"
}
```

### Input JSON example

`input.json`:

```json
{
  "format": "geotiff",
  "name": "test",
  "cache": "NONE",
  "noData": 0.0,
  "backend": {
    "type": "hadoop",
    "path": "input"
  }
}
```

### Backend JSON examples (local fs)

```json
"backend": {
  "type": "hadoop",
  "path": "file:///Data/nlcd/tiles"
}
```

### Backend JSON example (hdfs)

```json
"backend": {
  "type": "hadoop",
  "path": "hdfs://nlcd/tiles"
}
```

### Backend JSON example (s3)

```json
"backend": {
  "type": "s3",
  "path": "s3://com.azavea.datahub/catalog"
}
```

### Backend JSON example (accumulo)

```json
"backend": {
  "type": "accumulo",
  "profile": "accumulo-gis",
  "path": "nlcdtable"
}
```

### Backend JSON example (set of PNGs into S3)

```json
"backend": {
  "type": "render",  
  "path": "s3://tms-bucket/layers/{name}/{z}-{x}-{y}.png"
}
```

### Backend JSON example (set of PNGs into hdfs or local fs)

```json
"backend": {
  "type": "render",  
  "path": "hdfs://path/layers/{name}/{z}-{x}-{y}.png"
}
```
