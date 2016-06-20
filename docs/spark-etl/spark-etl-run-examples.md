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


### Ingest tiles from local fs or hdfs into s3 storage command

`datasets.json`:

```json
[
   {  
      "name":"nlcd-tms",
      "ingestType":{  
         "format":"{geotiff | temporal-geotiff}",         
         "output":"s3",         
         "input":"hadoop"
      },
      "path":{  
         "input":"file:///Data/nlcd/tiles",
         "output":"s3://com.azavea.datahub/catalog"
      },
      "cache":"NONE",
      "ingestOptions":{           
         "reprojectMethod":"buffered",         
         "tileSize":256,         
         "pyramid":true,
         "resampleMethod":"nearest-neighbor",
         "keyIndexMethod":{  
            "type":"zorder"
         },
         "layoutScheme":"{tms | floating}",         
         "crs":"EPSG:3857"
      }
   }
]
```

### Ingest singleband tiles from local fs or hdfs into accumulo storage command

`datasets.json`:

```json
[
   {  
      "name":"nlcd-tms",
      "ingestType":{  
         "format":"{geotiff | temporal-geotiff}",         
         "output":"accumulo",         
         "input":"hadoop",
         "outputCredentials":"accumulo-gis"
      },
      "path":{  
         "input":"file:///Data/nlcd/tiles",
         "output":"nlcd-table"
      },
      "cache":"NONE",
      "ingestOptions":{           
         "reprojectMethod":"buffered",         
         "tileSize":256,         
         "pyramid":true,
         "resampleMethod":"nearest-neighbor",
         "keyIndexMethod":{  
            "type":"zorder"
         },
         "layoutScheme":"{tms | floating}",         
         "crs":"EPSG:3857"
      }
   }
]
```

### Ingest singleband tiles from local fs or hdfs into hadoop storage

`datasets.json`:

```json
[
   {  
      "name":"nlcd-tms",
      "ingestType":{  
         "format":"{geotiff | temporal-geotiff}",         
         "output":"hadoop",         
         "input":"hadoop"
      },
      "path":{  
         "input":"file:///Data/nlcd/tiles",
         "output":"hdfs://geotrellis-ingest/nlcd/"
      },
      "cache":"NONE",
      "ingestOptions":{           
         "reprojectMethod":"buffered",         
         "tileSize":256,         
         "pyramid":true,
         "resampleMethod":"nearest-neighbor",
         "keyIndexMethod":{  
            "type":"zorder"
         },
         "layoutScheme":"{tms | floating}",         
         "crs":"EPSG:3857"
      }
   }
]
```

### Ingest singleband tiles from local fs or hdfs into rendered set of PNGs in S3

`datasets.json`:

```json
[
   {  
      "name":"nlcd-tms",
      "ingestType":{  
         "format":"{geotiff | temporal-geotiff}",         
         "output":"render",         
         "input":"hadoop"
      },
      "path":{  
         "input":"file:///Data/nlcd/tiles",
         "output":"s3://tms-bucket/layers/{name}/{z}-{x}-{y}.png"
      },
      "cache":"NONE",
      "ingestOptions":{   
         "encoding":"geotiff",
         "reprojectMethod":"buffered",         
         "tileSize":256,         
         "pyramid":true,
         "resampleMethod":"nearest-neighbor",
         "keyIndexMethod":{  
            "type":"zorder"
         },
         "layoutScheme":"{tms | floating}",         
         "crs":"EPSG:3857"
      }
   }
]
```

### Ingest singleband tiles from local fs or hdfs into rendered set of PNGs in hdfs or local fs

`datasets.json`:

```json
[
   {  
      "name":"nlcd-tms",
      "ingestType":{  
         "format":"{geotiff | temporal-geotiff}",         
         "output":"render",         
         "input":"hadoop"
      },
      "path":{  
         "input":"file:///Data/nlcd/tiles",
         "output":"hdfs://path/layers/{name}/{z}-{x}-{y}.png"
      },
      "cache":"NONE",
      "ingestOptions":{   
         "encoding":"geotiff",
         "reprojectMethod":"buffered",         
         "tileSize":256,         
         "pyramid":true,
         "resampleMethod":"nearest-neighbor",
         "keyIndexMethod":{  
            "type":"zorder"
         },
         "layoutScheme":"{tms | floating}",         
         "crs":"EPSG:3857"
      }
   }
]
```
