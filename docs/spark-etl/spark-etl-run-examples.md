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

### Credentials JSON example

`credentials.json`:

```json
{
  "accumulo": [{
    "name": "accumulo-gis",
    "zookeepers": "zookeepers",
    "instance": "instance",
    "user": "user",
    "password": "password"
  }],
  "cassandra": [{
    "name": "cassandra-gis",
    "allowRemoteDCsForLocalConsistencyLevel": false,
    "localDc": "datacenter1",
    "usedHostsPerRemoteDc": 0,
    "hosts": "hosts",
    "replicationStrategy": "SimpleStrategy",
    "replicationFactor": 1,
    "user": "user",
    "password": "password"
  }],
  "s3": [],
  "hadoop": []
}
```

### Data sets JSON example (local fs)

`datasets.json`:

```json
[
   {  
      "name":"nlcd-tms",
      "ingestType":{  
         "format":"{geotiff | temporal-geotiff}",            
         "input":"file"
      },
      "path": "file:///Data/nlcd/tiles",
      "cache":"NONE",
      "ingestOptions":{           
         "reprojectMethod":"buffered",         
         "tileSize":256,         
         "pyramid":true,
         "resampleMethod":"nearest-neighbor",
         "keyIndexMethod":{  
            "type":"zorder"
         },
         "layoutScheme":"{zoomed | floating}",         
         "crs":"EPSG:3857"
      }
   }
]
```

### Data sets JSON example (hdfs)

`datasets.json`:

```json
[
   {  
      "name":"nlcd-tms",
      "ingestType":{  
         "format":"{geotiff | temporal-geotiff}",            
         "input":"hadoop"
      },
      "path": "hdfs://nlcd/tiles",
      "cache":"NONE",
      "ingestOptions":{           
         "reprojectMethod":"buffered",         
         "tileSize":256,         
         "pyramid":true,
         "resampleMethod":"nearest-neighbor",
         "keyIndexMethod":{  
            "type":"zorder"
         },
         "layoutScheme":"{zoomed | floating}",         
         "crs":"EPSG:3857"
      }
   }
]
```

### Ingest tiles into s3 storage command

`output.json`:

```json
{
  "ingestOutputType": {
    "output": "s3"
  },
  "path": "s3://com.azavea.datahub/catalog"
}
```

### Ingest tiles into accumulo storage command

`output.json`:

```json
{
  "ingestOutputType": {
    "output": "accumulo",
    "credentials": "accumulo-gis"
  },
  "path": "nlcdtable"
}
```

### Ingest tiles into hadoop storage

`output.json`:

```json
{
  "ingestOutputType": {
    "output": "hadoop"    
  },
  "path": "hdfs://geotrellis-ingest/nlcd/"
}
```

### Ingest tiles into rendered set of PNGs in S3

`output.json`:

```json
{
  "ingestOutputType": {
    "output": "render"    
  },
  "path": "s3://tms-bucket/layers/{name}/{z}-{x}-{y}.png"
}
```

### Ingest tiles into rendered set of PNGs in hdfs or local fs

`output.json`:

```json
{
  "ingestOutputType": {
    "output": "render"    
  },
  "path": "hdfs://path/layers/{name}/{z}-{x}-{y}.png"
}
```
