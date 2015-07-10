# GeoTrellis ETL

This project implements a plugin architecture for tile ingest sources and `RasterRDD` sinks which allows you to write
basic ETL code using GeoTrellis without having to specify the type and configuration of the input and output at compile time.

Ingested layer may be modified using any of the existing raster operations before being saved.

```scala
object GeoTrellisETL extends App {
  val etl = Etl[SpatialKey](args, S3Module, HadoopModule)

  implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL")
  val (id, rdd) = etl.ingest()
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
--ingest hadoop --format geotiff --cache NONE -I path="file:///Data/nlcd/tiles" \
--output s3 -O bucket=com.azavea.datahub key=catalog \
--layer nlcd-tms --crs EPSG:3857 --pyramid
```

Note that the arguments before the `$JAR` configure `SparkContext` and arguments after configure GeoTrellis ETL inputs and outputs.

### Command Line Arguments

 Option       | Description
------------- | -------------
ingest        | Name of ingest module to use (ex: s3, hadoop)
format        | Format of the tile files to be ingested (ex: geotiff)
ingestProps   | List of `key=value` pairs that will be passed to the ingest module as configuration
cache         | Spark RDD storage level to be used for caching (default: MEMORY_AND_DISK_SER)
layerName     | Layer name to provide as result of the ingest
crs           | Desired CRS for ingested layer. May trigger raster reprojection. (ex: EPSG:3857")
tileSize      | Pixel height and width of each tile in the ingested layer
output        | Name of output module to use (ex: s3, hadoop, accumulo)
outputProps   | List of `key=value` pairs that will be passed to the output module as configuration
clobber       | Overwrite the layer on save in output catalog
pyramid       | Pyramid the layer on save starting from current zoom level to zoom level 1

## Extension

In order to provide your own ingest or sink modules you must extend `IngestPlugin` (src/main/scala/geotrellis/spark/etl/IngestPlugin) and
`SinkPlugin` (src/main/scala/geotrellis/spark/etl/IngestPlugin) respectively. These subclasses must be registered in a Guice `Module` and provided
to the `Etl` constructor.