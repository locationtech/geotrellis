Ingesting Imagery with GeoTrellis
=================================

A common problem faced by those with large amounts of data is how it
can be stored so that it can quickly queried without reading the entire
dataset. The process of collecting data, transforming it into rasters of
a desirable format, and storage for later querying, we refer to simply as an
'ingest'. Over the years, the GeoTrellis maintainers have played around
with a few different strategies for defining and executing them. Long
time GeoTrellis users might recall the (now deprecated and removed) `spark-etl`
package (which allowed users to build monolithic Extract/Transform/Load programs
with a few formatting and projection options) and the more modular
`pipeline <./pipeline.html>`__ approach, borrowed from PDAL.

As often happens yesterday's wisdom has fallen out of favor
and the former ETL project is now seen as overly abstracted and unable to
conveniently deal with the specificity and subtlty of real life
ingests. If you're looking for the former ETL project, it has been archived
at https://github.com/geotrellis/etl.

While the `pipeline` approach remains viable, our recent work has tended to
abandon generic, one-size-fits-all approaches in favor of directly writing programs
with a few lower-level constructs. This approach has allowed us to handle
edge cases with a minimum of hair-pulling and to focus on creating an API
that conveniently expresses the ways that a program might interact with some
source of imagery.

To this end, we've introduced the `RasterSource` which lazily represents some
source of imagery. The format might be TIFFs or PNGs, images could be backed by
S3 or your local hard drive - the `RasterSource` interface is abstract with respect
to such details.

Below is an example ingest using `RasterSources`. Note that none of this code
is ingest-specific; that all of it can (and is) used outside of ETL workflows.


A Sample Ingest with RasterSources
----------------------------------

.. code:: scala

  import geotrellis.contrib.performance.conf.{GDALEnabled, IngestVersion}
  import geotrellis.contrib.vlm._
  import geotrellis.contrib.vlm.avro._
  import geotrellis.contrib.vlm.spark.{RasterSourceRDD, RasterSummary, SpatialPartitioner}
  import geotrellis.proj4._
  import geotrellis.raster.{DoubleCellType, MultibandTile}
  import geotrellis.raster.resample.Bilinear
  import geotrellis.spark._
  import geotrellis.spark.io._
  import geotrellis.spark.io.s3._
  import geotrellis.spark.io.index.ZCurveKeyIndexMethod
  import geotrellis.spark.tiling.{LayoutLevel, ZoomedLayoutScheme}

  import org.apache.spark.{SparkConf, SparkContext}
  import org.apache.spark.rdd.RDD
  import cats.syntax.option._

  implicit val sc: SparkContext = createSparkContext("IngestRasterSource", new SparkConf(true))

  /** Some constants for us to refer back to */

  // a name for the layer to be ingested
  val layerName = "my-rastersource-ingest"
  // the projection we'd like things to be tiled in
  val targetCRS = WebMercator
  // an interpolation method
  val interp = Bilinear
  // the scheme for generating tile layouts
  val layoutScheme = ZoomedLayoutScheme(targetCRS, tileSize = 256)

  // Here, we parallelize a list of URIs and then turn them each into RasterSources
  // Note that reprojection/celltype manipulation is something that RasterSources allow us to do directly
  val sourceRDD: RDD[RasterSource] =
    sc.parallelize(paths, paths.length)
      .map(uri => getRasterSource(uri, gdalEnabled).reproject(targetCRS, interp).convert(DoubleCellType): RasterSource)
      .cache()

  // A RasterSummary is necessary for writing metadata; we can get one from an RDD
  val summary = RasterSummary.fromRDD[RasterSource, Long](sourceRDD)

  // levelFor gives us the zoom and LayoutDefinition which most closely matches the computed Summary's Extent and CellSize
  val LayoutLevel(zoom, layout) = summary.levelFor(layoutScheme)

  // This is the actual in (spark, distributed) memory layer
  val contextRDD = RasterSourceRDD.tiledLayerRDD(sourceRDD, layout, rasterSummary = summary.some)

  // A reference to the attribute store for this layer
  val attributeStore = S3AttributeStore(catalogURI.getBucket, catalogURI.getKey)

  // Build a layer writer
  val writer = S3LayerWriter(attributeStore)

  // Actually write out the RDD constructed and transformed above
  writer.write(LayerId(layerName, zoom), contextRDD, ZCurveKeyIndexMethod)

