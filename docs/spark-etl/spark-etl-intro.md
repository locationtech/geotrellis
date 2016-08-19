# GeoTrellis ETL

When working with GeoTrellis often the first task is to load a set of rasters to perform reprojection, mosaicing and
pyramiding before saving them as a GeoTrellis layer. It is possible, and not too difficult, to use core GreoTrellis
features to write a program to accomplish this task. However, after writing a number of such programs we noticed two
patterns emerge:

  - Often an individual ETL process will require some modification that is orthogonal to the core ETL logic
  - When designing an ETL process it is useful to first run it a smaller dataset, perhaps locally, as a verification
  - Once written it would be useful to re-run the same ETL process with different input and outputs storage mediums

To assist these patterns `spark-etl` project implements a plugin architecture for tile input sources and output sinks
which allows you to write a compact ETL program without having to specify the type and the configuration of
the input and output at compile time. The ETL process is broken into three stages: `load`, `tile`, and `save`.
This affords an opportunity to modify the dataset using any of the GeoTrellis operations in between the stages.

## Sample ETL Application

```scala
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.Etl
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.spark.util.SparkUtils
import geotrellis.vector.ProjectedExtent

import org.apache.spark.SparkConf

object GeoTrellisETL {
  def main(args: Array[String]): Unit = {
    implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL", new SparkConf(true))

    try {
      EtlConf(args) foreach { conf =>
        /* parse command line arguments */
        val etl = Etl(conf, modules)
        /* load source tiles using input module specified */
        val sourceTiles = etl.load[I, V]
        /* perform the reprojection and mosaicing step to fit tiles to LayoutScheme specified */
        val (zoom, tiled) = etl.tile(sourceTiles)
        /* save and optionally pyramid the mosaiced layer */
        etl.save[K, V](LayerId(etl.input.name, zoom), tiled)
      }
    } finally {
      sc.stop()
    }
  }
}
```

### User defined ETL configs

The above sample application can be placed in a new SBT project that
has a dependency on `"com.azavea.geotrellis" %% "geotrellis-spark-etl" % s"$VERSION"`
in addition to dependency on `spark-core`.  and built into an assembly
with `sbt-assembly` plugin. You should be careful to include a
`assemblyMergeStrategy` for sbt assembly plugin as it is provided in
[spark-etl build file](build.sbt).

At this point you would create a seperate `App` object for each one
of your ETL configs.

### Build-in ETL configs

For convinence and as an example the `spark-etl` project provides two
`App` objects that perform vanilla ETL:
- geotrellis.spark.etl.SinglebandIngest
- geotrellis.spark.etl.MultibandIngest

You may use them by building an assembly jar of `spark-etl` project as follows:

```bash
cd geotrellis
./sbt
sbt> project spark-etl
sbt> assembly
```

The assembly jar will be placed in `geotrellis/spark-etl/target/scala-2.10` directory.

## Running the Spark Job

For maximum flexibility it is desirable to run spark jobs with
`spark-submit`. In order to achieve this `spark-core` dependency must
be listed as `provided` and `sbt-assembly` plugin used to create the
fat jar as described above. Once the assembly jar is read outputs and
inputs can be setup through command line arguments like so:

```bash
#!/bin/sh
export JAR="geotrellis-etl-assembly-1.0.0-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.SinglebandIngest \
--master local[*] \
--driver-memory 2G \
$JAR \
--backend-profiles "file://backend-profiles.json" \
--input "file://input.json" \
--output "file://output.json"
```

Note that the arguments before the `$JAR` configure `SparkContext`
and arguments after configure GeoTrellis ETL inputs and outputs.

### Examples

[Extended run-scripts examples available here](./spark-etl-run-examples.md).

### Command Line Arguments

 Option          | Description
-----------------| -------------
backend-profiles | Path to a json file (local fs / hdfs) with credentials for ingest datasets (required field)
input            | Path to a json file (local fs / hdfs) with datasets to ingest, with optional credentials
output           | Path to a json file (local fs / hdfs) with output backend params to ingest, with optional credentials

#### Backend profiles JSON description

```json
{
  "backend-profiles": [{
    "name": "accumulo-name",
    "type": "accumulo",
    "zookeepers": "zookeepers",
    "instance": "instance",
    "user": "user",
    "password": "password"
  },
  {
    "name": "cassandra-name",
    "type": "cassandra",
    "allowRemoteDCsForLocalConsistencyLevel": false,
    "localDc": "datacenter1",
    "usedHostsPerRemoteDc": 0,
    "hosts": "hosts",
    "replicationStrategy": "SimpleStrategy",
    "replicationFactor": 1,
    "user": "user",
    "password": "password"
  }]
}
```

Sets of *named* profiles for each backend.

#### Output JSON description

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

Key                 | Value
--------------------|----------------
backend             | backend description is presented below
breaks              | breaks string for `render` output (optional field)
partitions          | partitions number during pyramid build
reprojectMethod     | buffered | per-tile
cellSize            | sell size 
encoding            | png | geotiff for `render` output
tileSize            | tile size (optional field)
layoutExtent        | layout extent (optional field)
resolutionThreshold | resolution for user defined Layout Scheme (optional field)
pyramid             | true | false - ingest with / with out building pyramid 
resampleMethod      | nearest-neighbor | bilinear | cubic-convolution | cubic-spline | lanczos
keyIndexMethod      | key index method (zorder | row-major | hilbert)
layoutScheme        | tms | floating (optional field) (optional field)
cellType            | int8 | int16 | etc... (optional field)
crs                 | destination crs name (example: EPSG:3857) (optional field)


##### Backend JSON description

Key              | Value
-----------------|----------------
type             | Input backend type (file / hadoop / s3 / accumulo / cassandra)
path             | Input path (local path / hdfs), or s3:// url
profile          | Profile name to use for input

###### Supported Layout Schemes

Layout Scheme    | Options
-----------------|----------------
zoomed           | zoomed layout scheme
floating         | floating layout scheme in a native projection

###### KeyIndexMethod JSON description

Key                | Options
-------------------|----------------
type               | zorder | row-major | hilbert
temporalResolution | temporal resolution for temporal indexing (optional field)
timeTag            | time tag name for input geotiff tiles (optional field)
timeFormat         | time format to parse time stored in time tag geotiff tag (optional field)
 
#### Input JSON description

```json
[{
  "format": "geotiff",
  "name": "test",
  "cache": "NONE",
  "noData": 0.0,
  "backend": {
    "type": "hadoop",
    "path": "input"
  }
}]
```

Key              | Value
-----------------|----------------
format           | Format of the tile files to be read (ex: geotiff)
name             | Input dataset name
cache            | Spark RDD cache strategy
noData           | NoData value

###### Supported Formats

Format           | Options
-----------------|----------------
geotiff          | Spatial ingest
temporal-geotiff | Temporal ingest

###### Supported Inputs

Input     | Options
----------|----------------
hadoop    | path (local path / hdfs)
s3        | s3:// url

###### Supported Outputs

Output    | Options
----------|----------------
hadoop    | path
accumulo  | table name
cassandra | table name with keysapce (keyspace.tablename)
s3        | s3:// url
render    | path

##### Accumulo Output

Accumulo output module has two write strategies:
- `hdfs` strategy uses Accumulo bulk import
- `socket` strategy uses Accumulo `BatchWriter`
When using `hdfs` strategy `ingestPath` argument will be used as
the temporary directory where records will be written for use by
Accumulo bulk import. This directory should ideally be an HDFS path.

#### Layout Scheme

GeoTrellis is able to tile layers in either `ZoomedLayoutScheme`,
matching TMS pyramid, or `FloatingLayoutScheme`, matching the native
resolution of input raster. These alternatives may be selecting by
using the `layoutScheme` option.

Note that `ZoomedLayoutScheme` needs to know the world extent, which
it gets from the CRS, in order to build the TMS pyramid layout. This
will likely cause resampling of input rasters to match the resolution
of the TMS levels.

On other hand `FloatingLayoutScheme` will discover the native resolution
and extent and partition it by given tile size without resampling.

##### User Defined Layout

You may bypass the layout scheme logic by providing `layoutExtent`,
`cellSize`, and `cellType` instead of the `layoutScheme` option. Together
with `tileSize` option this is enough to fully define the layout and
start the tiling process.

#### Reprojection

`spark-etl` project supports two methods of reprojection: `buffered` and
`per-tile`. They provide a trade-off between accuracy and flexibility.

Buffered reprojection method is able to sample pixels past the tile
boundaries by performing a neighborhood join. This method is the default
and produces the best results. However it requires that all of the
source tiles share the same CRS.

Per tile teproject method can not consider pixels past the individual
tile boundaries, even if they exist elsewhere in the dataset. Any pixels
past the tile boundaries will be as `NODATA` when interpolating. This
restriction allows for source tiles to have a different projections per tile.
This is an effective way to unify the projections for instance when
projection from multiple UTM projections to WebMercator.

### Rendering a Layer

`render` output module is different from other modules in that it does not
save a GeoTrellis layer but rather provides a way to render a layer, after
tiling and projection, to a set of images. This is useful to either verify
the ETL process or render a TMS pyramid.

The `path` module argument is actually a path template, that allows the following substitution:
  - `{x}` tile x coordinate
  - `{y}` tile y coordinate
  - `{z}` layer zoom level
  - `{name}` layer name

A sample render output configuration template could be:
 ```json
   "path": "s3://tms-bucket/layers/{name}/{z}-{x}-{y}.png",
   "ingestType":{  
     "format":"geotiff",     
     "output":"render"
   }
 ```

## Extension

In order to provide your own input or output modules you must extend
[`InputPlugin`](src/main/scala/geotrellis/spark/etl/InputPlugin) and
[`OutputPlugin`](src/main/scala/geotrellis/spark/etl/OutputPlugin)
and register them in the `Etl` constructor via a `TypedModule`.

## Input JSON validation schema

* [--backend-profiles](/spark-etl/src/main/resources/backend-profiles-schema.json) 
* [--input](/spark-etl/src/main/resources/input-schema.json)
* [--output](/spark-etl/src/main/resources/output-schema.json)
