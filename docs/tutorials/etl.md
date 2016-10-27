# Local ETL Tutorial #

This brief tutorial describes how to use GeoTrellis' ETL functionality to create a GeoTrellis catalog.
We will accomplish this in three steps:
1) We will build the ETL assembly from code in the GeoTrellis source tree
2) We will compose JSON configuration files describing the input and output data
and 3) We will exercise the ingested data using a simple project.

It is assumed throughout this tutorial that Spark 2.0.0 or greater is installed,
that the GDAL command line tools are installed,
and that the GeoTrellis source tree has been locally cloned.

## Build the Assembly ##

Navigate into the GeoTrellis source tree, build the assembly, and copy it to some convenient location:

```console
cd geotrellis
./sbt "project spark-etl" assembly
cp spark-etl/target/scala-2.11/geotrellis-spark-etl-assembly-1.0.0.jar /tmp
```

Although in this tutorial we have chosen to build this assembly directly from the GeoTrellis source tree,
in some applications it may be desirable to create a class in one's own code base that uses `geotrellis.spark.etl.SinglebandIngest` or `geotrellis.spark.etl.MultibandIngest` and generate an assembly of that.
See the [Chatta Demo](https://github.com/geotrellis/geotrellis-chatta-demo/blob/94ae99269236610e66841893990860b7760e3663/geotrellis/src/main/scala/geotrellis/chatta/ChattaIngest.scala) for an example.

## Compose JSON Configuration Files ##

The configuration files that we create in this section are intended for use with a single multiband GeoTiff image.
Three JSON files are required: one describing the input data, one describing the output data, and one describing the backend(s) in which the catalog should be stored.
Please see [the ETL documentation](../spark-etl/spark-etl-run-examples.md) for more information on this topic.

First, retile the source raster.
This is not strictly required if the source image is small enough (probably less than 2GB),
but is still good practice even if it is not required.

```console
gdal_retile.py source.tif -of GTiff -co compress=deflate -ps 256 256 -targetDir /tmp/rasters
```

The result of this command is a collection of smaller GeoTiff tiles in the directory `/tmp/rasters`.

We will now create three files in the `/tmp/json` directory: `input.json`, `output.json`, and `backend-profiles.json`.

Here is `input.json`:
```json
[{
    "format": "multiband-geotiff",
    "name": "example",
    "cache": "NONE",
    "backend": {
        "type": "hadoop",
        "path": "file:///tmp/rasters"
    }
}]
```

The value `multiband-geotiff` is associated with the `format` key.
That is required if you want to access the data as an RDD of key, `MultibandTile` pairs.
Making that value `geotiff` instead of `multiband-geotiff` will result in `Tile` data.
The value `example` associated with the key `name` gives the name of the layer(s) that will be created.
The `cache` key gives the Spark caching strategy that will be used during the ETL process.
Finally, the value associated with the `backend` key specifies where the data should be read from.
In this case, the source data are stored in the directory `/tmp/rasters` on local filesystem and accessed via Hadoop.

Here is the `output.json` file:
```json
{
    "backend": {
        "type": "hadoop",
        "path": "file:///tmp/catalog/"
    },
    "reprojectMethod": "buffered",
    "pyramid": true,
    "tileSize": 256,
    "keyIndexMethod": {
        "type": "zorder"
    },
    "resampleMethod": "cubic-spline",
    "layoutScheme": "zoomed",
    "crs": "EPSG:3857"
}
```

That file says that the catalog should be created on the local filesystem in the directory `/tmp/catalog` using Hadoop.
The source data is pyramided so that layers of zoom level 0 through 12 are created in the catalog.
The tiles are 256-by-256 pixels in size and are indexed in according to Z-order.
Bicubic resampling (spline rather than convolutional) is used in the reprojection process, and the CRS associated with the layers is EPSG 3857 (Web Mercator).

Here is the `backend-profiles.json` file:
```json
{
    "backend-profiles": []
}
```

In this case, we did not need to specify anything since we are using Hadoop for both input and output.
It happens that Hadoop only needs to know the path to which it should read or write, and we provided that information in the `input.json` and `output.json` files.
Other backends such as Cassandra and Accumulo information to be provided in the `backend-profiles.json` file.

Now with all of the files that we need in place
(`/tmp/geotrellis-spark-etl-assembly-1.0.0.jar`, `/tmp/input.json`, `/tmp/output.json`, `/tmp/backend-profiles.json`, and `/tmp/rasters/*.tif`)
we are ready to perform the ingest.
That can be done by typing:

```console
$SPARK_HOME/bin/spark-submit \
   --class geotrellis.spark.etl.MultibandIngest \
   --master 'local[*]' \
   --driver-memory 16G \
   /tmp/geotrellis-spark-etl-assembly-1.0.0.jar \
   --input "file:///tmp/json/input.json" \
   --output "file:///tmp/json/output.json" \
   --backend-profiles "file:///tmp/json/backend-profiles.json"
```

There should now be an directory called `/tmp/catalog` that did not exist before.

## Exercise the Catalog ##

Clone or download [this example code](https://github.com/jamesmcclain/GeoWaveIngest/tree/scratch-work)
(a zipped version of which can be downloaded from [here](https://github.com/jamesmcclain/GeoWaveIngest/archive/scratch-work.zip)).
Once obtained, the code can be built like this:

```console
cd GeoWaveIngest
./sbt "project scratch" assembly
cp scratch/target/scala-2.11/scratch-assembly-0.jar /tmp
```

The code can be run by typing:

```console
mkdir -p /tmp/tif
$SPARK_HOME/bin/spark-submit \
   --class com.azavea.geotrellis.scratch.Scratch \
   --master 'local[*]' \
   --driver-memory 16G \
   scratch/target/scala-2.11/scratch-assembly-0.jar /tmp/catalog example 12
```

In the block above, `/tmp/catalog` is an HDFS URI pointing to the location of the catalog, `example` is the layer name, and `12` is the layer zoom level.
After running the code, you should find a number of images in `/tmp/tif` which are GeoTiff renderings of the tiles of the raw layer, as well as the layer with various transformations applied to it.
