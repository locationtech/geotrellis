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

object GeoTrellisETL extends App {
  implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL", new SparkConf(true))

  /* parse command line arguments */
  val etl = Etl(args)
  /* load source tiles using input module specified */
  val sourceTiles = etl.load[ProjectedExtent, Tile]
  /* perform the reprojection and mosaicing step to fit tiles to LayoutScheme specified */
  val (zoom, tiled) = etl.tile(sourceTiles)
  /* save and optionally pyramid the mosaiced layer */
  etl.save(LayerId(etl.conf.layerName(), zoom), tiled, ZCurveKeyIndexMethod)

  sc.stop()
}
```

### User defined ETL jobs

The above sample application can be placed in a new SBT project that
has a dependency on `"com.azavea.geotrellis" %% "geotrellis-spark-etl" % s"$VERSION"`
in addition to dependency on `spark-core`.  and built into an assembly
with `sbt-assembly` plugin. You should be careful to include a
`assemblyMergeStrategy` for sbt assembly plugin as it is provided in
[spark-etl build file](build.sbt).

At this point you would create a seperate `App` object for each one
of your ETL jobs.

### Build-in ETL jobs

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
export JAR="geotrellis-etl-assembly-0.10-SNAPSHOT.jar"

spark-submit \
--class geotrellis.spark.etl.SinglebandIngest \
--master local[*] \
--driver-memory 2G \
$JAR \
--input hadoop --format geotiff --cache NONE -I path="file:///Data/nlcd/tiles" \
--output s3 -O bucket=com.azavea.datahub key=catalog \
--layer nlcd-tms --crs EPSG:3857 --pyramid --layoutScheme tms
```

Note that the arguments before the `$JAR` configure `SparkContext`
and arguments after configure GeoTrellis ETL inputs and outputs.

Extended run-scripts examples available [here](./spark-etl-run-examples.md).

### Command Line Arguments

 Option       | Description
------------- | -------------
input         | Name of input module to use (ex: s3, hadoop)
format        | Format of the tile files to be read (ex: geotiff)
inputProps    | List of `key=value` pairs that will be passed to the input module as configuration
cache         | Spark RDD storage level to be used for caching (default: MEMORY_AND_DISK_SER)
layerName     | Layer name to provide as result of the input
crs           | Desired CRS for input layer. May trigger raster reprojection. (ex: EPSG:3857")
tileSize      | Pixel height and width of each tile in the input layer
layoutScheme  | Scheme to be used to determine raster resolution and extent (ex: tms, floating)
layoutExtent  | Explicit alternative to use of `layoutScheme` (format: xmin,ymin,xmax,ymax)
reproject     | Reproject method to use during tiling (ex: buffered or per-tile)
cellSize      | Width and Height of each pixel (format: width,height)
cellType      | Value of type of the target raster (ex: bool, int8, int32, int64, float32, float64)
output        | Name of output module to use (ex: s3, hadoop, accumulo)
outputProps   | List of `key=value` pairs that will be passed to the output module as configuration
pyramid       | Pyramid the layer on save starting from current zoom level to zoom level 1
histogram     | Save histogram to the output AttributeStore for every saved layer

#### Supported Inputs

Input     | Options
----------|----------------
hadoop    | path (local path / hdfs)
s3        | bucket, key, splitSize

#### Supported Outputs

Output    | Options
----------|----------------
hadoop    | path
accumulo  | instance, zookeeper, user, password, table, strategy={hdfs|socket}, ingestPath
s3        | bucket, key
render    | path, encoding=(`geotiff` or `png`), breaks='{limit}:{RGBA};{limit}:{RGBA};...'

#### Supported Formats

Format           | Options
-----------------|----------------
geotiff          | spatial ingest
temporal-geotiff | temporal ingest

#### Supported Layout Schemes

Layout Scheme    | Options
-----------------|----------------
tms              | zoomed layout scheme
floating         | floating layout scheme in a native projection

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
 `--output render -O encodering=png path=s3://tms-bucket/layers/{name}/{z}-{x}-{y}.png`.

## Extension

In order to provide your own input or output modules you must extend
[`InputPlugin`](src/main/scala/geotrellis/spark/etl/InputPlugin) and
[`OutputPlugin`](src/main/scala/geotrellis/spark/etl/OutputPlugin)
and register them in the `Etl` constructor via a `TypedModule`.
