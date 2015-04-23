# Spark

This subproject covers integration of GeoTrellis and Spark. The integration consists mainly of extending Spark's concept of a resilient distributed dataset (RDD) to rasters. With this, GeoTrellis can take advantage of Spark's distributed computing model to operate on rasters too large to process or analyze on a single machine or hold in memory all at once.

## RasterRDD

The RasterRDD abstraction is the basis for GeoTrellis' integration and usage of the Spark programming model. RasterRDDs are key-value/paired RDDs where the key consists of some combination of a _spatial_ and/or _temporal_ component and the value is a traditional raster tile. Because RasterRDDs are actually RDDs, the same transformations, actions, and methods available on traditional RDDs are available on RasterRDDs. RasterRDDs offer additional functionality specific to raster operations in addition the traditional methods that make them appropriate to use.

*Spatial keys* are keys that index on space for a tile in a RasterRDD. Encoding this information in the key, and using the same value in the data-store, allows for efficient hydrating of RDDs from a datastore by requiring only loading the minimum number of tiles to be loaded into an RDD for a given operation. For instance, given raster data that covers the entire world that has been ingested, a zonal summary operation that only spans the city of Philadelphia would only need to load tiles that cover the extent of Philadelphia. Indexing the RDD with that information allows for fast filtering and to be able to take advantage of data locality.

*Space-Time Keys* expand on the spatial key by also adding a temporal component. This addition of a temporal component makes working with time-series rasters efficient and easy by allowing for fast raster hydration not only based on space, but time as well.

## Ingest

To construct RasterRDDs from existing data sources (GeoTIFFs, etc.) an ingestion step is required. The purpose of ingestion is to transform, reproject, and tile the original datasource to make it more amenable to constructing RDDs that are efficiently indexed/keyed. Currently the ingestion process covers (XYZ) and requires some type of datastore like Accumulo.

Ingestion involves a few steps:

1. Load GeoTIFFs
2. Reproject to projection you want
3. Determine metadata for the layer (CRS, extent, tilelayout, etc.)
4. Combine reprojected tiles into 256x256 tiles
5. Optionally, pyramid the tiles at different zoom levels
6. Push tiles into a datastore in addition to metadata about the raster

Ingestion can be done using spark-submit with the appropriate arguments for the type of ingestion. For example, to ingest a set of geotiffs into accumulo, the following command could be used:

```bash
spark-submit --class geotrellis.spark.ingest.AccumuloIngestCommand
             $GEOTRELLIS_SPARK_JAR
			 --instance geotrellis-accumulo-cluster
			 --user root
			 --password secret
			 --zookeeper localhost
			 --crs EPSG:3857
			 --pyramid true
			 --layerName nexmonth
			 --table nexmonth
			 --input /tmp/one-month-tiles
```
This command ingests the set of geotiffs located at `/tmp/one-month-tilles`, reprojects the tiles to web-mercator (EPSG:3857), and splits the raster into 256x256 tiles. It also pyramids the tiles at different zoom levels. Next, it saves the raster tiles into accumulo in the table `nexmonth`. Additionaly, some metadata about the raster(s) are stored in accumulo. This metadata includes information about the projection, extent of the RasterRDD, cell type, and tile layout.

Once the ingestion process is over, it is now possible to use accumulo to construct RasterRDDs to perform operations on. These RDDs operate just like any other spark RDD. To play with the spatial data interactively start `spark-shell` using the GeoTrellis spark assembly jar.

```bash
spark-shell --jars /path/to/geotrellis-spark-assembly-0.10.0-SNAPSHOT.jar
```

Once in the scala spark shell you can load the data into an RDD

```scala
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.io.accumulo._

import org.apache.accumulo.core.client.security.tokens.PasswordToken

// Create an implicit for the spark context that will be passed around
implicit val _sc = sc

// Get accumulo instance and the catalog.
val accumulo = AccumuloInstance("geotrellis-accumulo-cluster", "localhost", "root", new PasswordToken("secret"))
def catalog = accumulo.catalog

// Create a RasterRDD of nexMonth data
val nexMonth = catalog.load[SpatialKey](LayerId("nexmonth", 5))
```

## Tiling
Data sources for rasters to be ingested may not be in the most appropriate projection for analysis or display to users. Additionally, the existing ingested tiles may not be cut appropriately to be turned into a RasterRDD. GeoTrellis supports reprojecting ingested rasters and constructing appropriate 256x256 tiles to construct a RasterRDD.

`geotrellis.spark.ingest.Tiler` handles the cutting, merging, and reprojecting of tiling during the ingest process, producing a RasterRDD with an appropriate key.

## Operations - Transformations
Operations on RasterRDDs work similarly to operations on normal raster. Almost all operations on RasterRDDs user operations defined on the normal Raster that GeoTrellis provides. The difference is how operations are divided up so that different parts of each RasterRDD can be operated on in parallel.

Similar to (RASTER STUFF) these operations are divided according to categories that represent a certain category
- `geotrellis.spark.op.elevation` includes operations related to elevation data, including calculating the slope, aspect, and hillshade for cells
- `geotrellis.spark.op.focal` operations operate on a neighborhood of cells to calculate some new value for a single cell (e.g. `focalMax` assigns the value to a cell based on the values of its neighboring cells)
- `geotrellis.spark.op.global` perform some operation that transform the whole raster and is currently limited to `verticalFlip`
- `geotrellis.spark.op.local` these operations require only the value of a single cell to perform the operation (e.g. adding a constant to every cell)
- `geotrellis.spark.op.stats` operations which return a statistical summary of the raster (e.g. histogram)
- `geotrellis.spark.op.zonal` given a zone, these operations perform some calculation based on the raster cells within the zone, such as computing a histogram for a selection of cells

All operations are available as methods on a RasterRDD after importing the appropriate implicit methods (e.g. for focal methods `geotrellis.spark.op.focal._`) into scope.

```scala

// Importing implicitly adds operations to RasterRDDs
import geotrellis.raster.op.focal._
import geotrellis.raster.op.elevation._

// focalMax
val nexFocalMax = nexMonth.focalMax(Square(1))

// Slope
val nexSlope = nexMonth.slope

// histogram (doesn't return an RDD!)
val nexHistogram = nexMonth.histogram
val nexMean = nexHistogram.getMean
```

Besides histogram, almost all operations on RasterRDDs return a new RDD. However, many of these operations will materialize immediately which makes these operations only partially analagoous to a _transformation_ in spark terminology. Triggering RDDs to materialize requires some _action_ to be called. This could be calling `take(1)` on a raster to take a single tile or saving a new raster to accumulo or disk. The code excerpt above shows examples of both of these operations.
