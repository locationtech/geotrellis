# GeoTrellis ETL

This project implements a plugin architecture for tile input sources and `RasterRDD` sinks which allows you to write
basic ETL code using GeoTrellis without having to specify the type and configuration of the input and output at compile time.

Input layer may be modified using any of the existing raster operations before being saved.

```scala
import geotrellis.spark.utils.SparkUtils

object GeoTrellisETL extends App {
  val etl = Etl[SpatialKey](args)

  implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL")
  val (id, rdd) = etl.load()
  val result = rdd.localAdd(1)
  etl.save(id, result, ZCurveKeyIndexMethod)
  sc.stop()
}
```

## Running the Spark Job

For maximum flexibility it is desirable to run spark jobs with `spark-submit`. In order to achieve this `spark-core`
dependency must be listed as `provided` and `sbt-assembly` plugin used to create the fat jar, with all dependencies included but `spark-core`.
Once the assembly jar is read outputs and inputs can be setup through command line arguments like so:

```sh
#!/bin/sh
export JAR="geotrellis-etl-assembly-0.1-SNAPSHOT.jar"

spark-submit \
--class GeoTrellisETL \
--master local[*] \
--driver-memory 2G \
$JAR \
--input hadoop --format geotiff --cache NONE -I path="file:///Data/nlcd/tiles" \
--output s3 -O bucket=com.azavea.datahub key=catalog \
--layer nlcd-tms --crs EPSG:3857 --pyramid
```

Note that the arguments before the `$JAR` configure `SparkContext` and arguments after configure GeoTrellis ETL inputs and outputs.

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
cellSize      | Width and Height of each pixel (format: width,height)
cellType      | Value of type of the target raster (ex: bool, int8, int32, int64, float32, float64)
output        | Name of output module to use (ex: s3, hadoop, accumulo)
outputProps   | List of `key=value` pairs that will be passed to the output module as configuration
clobber       | Overwrite the layer on save in output catalog
pyramid       | Pyramid the layer on save starting from current zoom level to zoom level 1
histogram     | Save histogram to the output AttributeStore for every saved layer


#### Supported Inputs

Output    | Options
----------|----------------
hadoop    | path
s3        | bucket, key, splitSize

#### Supported Outputs

Output    | Options
----------|----------------
hadoop    | path
accumulo  | instance, zookeeper, user, password, table
s3        | bucket, key
render    | path, format=(`geotiff` or `png`), breaks='{limit}:{RGBA};{limit}:{RGBA};...'


## Extension

In order to provide your own input or output modules you must extend `InputPlugin` (src/main/scala/geotrellis/spark/etl/InputPlugin) and
`OutputPlugin` (src/main/scala/geotrellis/spark/etl/OutputPlugin) respectively. These subclasses must be registered in a Guice `Module` and provided
to the `Etl` constructor.

Once defined you can pass the list of modules to be used for ETL like so:

```scala
val etl = Etl[SpatialKey](args, Etl(args, List(s3.S3Module, hadoop.HadoopModule)))
```

## Layout Schemes

GeoTrellis is able to tile layers in either `ZoomedLayoutScheme`, matching TMS pyramid, or `FloatingLayoutScheme`, matching the native resolution of input raster.

These alternatives may be selecting by using the `layoutScheme` option.

Note that `ZoomedLayoutScheme` needs to know the world extent, which it gets from the CRS, in order to build the TMS pyramid layout.
This will likely cause resampling of input rasters to match the resolution of the TMS levels.

On other hand `FloatingLayoutScheme` will discover the native resolution and extent and partition it by given tile size without resampling.

### User Defined Layout

You may bypass the metadata collection step by providing `layoutExtent`, `cellSize`, and `cellType` instead of the `layoutScheme` option.
Together with `tileSize` option this is enough to fully define the raster metadata and start the tiling process.