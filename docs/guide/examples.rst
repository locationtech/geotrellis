Example Archive
***************

This is a collection of code snippets that represent common GeoTrellis tasks.

Get a Layer from a GeoTiff on S3
==================================

**Motivation:** You have a potentially large GeoTiff on S3 that you'd
like to form a proper keyed GeoTrellis layer out of. While you're at it,
you'd like to change the projection of the imagery too.

New as of GeoTrellis 1.1, ``S3GeoTiffRDD`` provides a powerful set of
functions for fetching Tiles from the cloud. Here's an example that
encompasses your desired work-flow:

.. code-block:: scala

   def layerFromS3(bucket: String, key: String)(implicit sc: SparkContext): (Int, TileLayerRDD[SpatialKey]) = {
    /* Extract Tiles efficiently from a remote GeoTiff. */
    val ungridded: RDD[(ProjectedExtent, Tile)] =
      S3GeoTiffRDD.spatial(bucket, key)

    /* Say it was in WebMercator and we want it in ConusAlbers. */
    val reprojected: RDD[(ProjectedExtent, Tile)] =
      ungridded.reproject(ConusAlbers)

    /* Our layer will need metadata. Luckily, this can be derived mostly for free. */
    val (zoom, meta): (Int, TileLayerMetadata[SpatialKey]) =
      TileLayerMetadata.fromRDD(reprojected, ZoomedLayoutScheme(ConusAlbers))

    /* Recut our Tiles to form a proper gridded "layer". */
    val gridded: RDD[(SpatialKey, Tile)] = reprojected.tileToLayout(meta)

    (zoom, ContextRDD(gridded, meta))
   }

Want to pyramid up to zoom-level 0 while you're at it?

.. code-block:: scala

   def pyramid(bucket: String, key: String)(implicit sc: SparkContext): Stream[(Int, TileLayerRDD[SpatialKey])] = {
    val (zoom, layer) = layerFromS3(bucket, key)

    Pyramid.levelStream(layer, ZoomedLayoutScheme(ConusAlbers), zoom)
   }

Get the ``Extent`` of a ``Tile``
================================

**Motivation:** Given a pair ``(SpatialKey, Tile)``, you'd like to know the
geographic area on the earth that they cover (the ``Extent``). This ``Extent``
has many uses within GeoTrellis.

This solution assumes you have a ``LayoutDefinition`` on hand. If you're working
with a ``TileLayerRDD``, as many GeoTrellis operations do, then grabbing one is just:

.. code-block:: scala

   val layer: TileLayerRDD[SpatialKey] = ...  /* Output of previous work */

   val layout: LayoutDefinition = layer.metadata.layout

Now to find the ``Extent`` of the key/tile pair:

.. code-block:: scala

   val (key, tile): (SpatialKey, Tile) = ...

   val extent1: Extent = key.extent(layout)        /* As of GeoTrellis 1.2 */
   val extent2: Extent = layout.mapTransform(key)  /* For versions < 1.2 */

Create a Layer from a single ``Tile``
=====================================

**Motivation:** You've gotten a single ``Tile`` as output from some GeoTrellis
function. Now you'd like to promote it as-is as a GeoTrellis layer, perhaps for
easy output.

First, it is assumed that you know the projection (``CRS``) that your ``Tile``
is in, and that you've calculated its ``Extent``. If so, you can construct a
``ProjectedRaster``:

.. code-block:: scala

   val tile: Tile = ...
   val crs: CRS = ...
   val extent: Extent = ...

   val raster: ProjectedRaster[Tile] = ProjectedRaster(Raster(tile, extent), crs)

This is the minimum amount of information required to construct a Layer. A function
that does that could look like this:

.. code-block:: scala

   import geotrellis.raster._
   import geotrellis.spark._
   import geotrellis.spark.SpatialKey._  /* To get a `Boundable` instance for `SpatialKey` */
   import geotrellis.spark.tiling._
   import geotrellis.vector._
   import org.apache.spark._
   import org.apache.spark.rdd._

   /** Convert an in-memory `Tile` into a GeoTrellis layer. */
   def toLayer(pr: ProjectedRaster[Tile])(implicit sc: SparkContext): TileLayerRDD[SpatialKey] = {

     val layer: RDD[(ProjectedExtent, Tile)] =
       sc.parallelize(List((ProjectedExtent(pr.raster.extent, pr.crs), pr.raster.tile)))

     val scheme: LayoutScheme = ZoomedLayoutScheme(pr.crs)

     /* The metadata, plus the zoom level corresponding to the size of the Tile.
      * We don't need the zoom level here, but it deserves a note.
      */
     val meta: (Int, TileLayerMetadata[SpatialKey]) = layer.collectMetadata(scheme)

     ContextRDD(layer.tileToLayout[SpatialKey](meta._2), meta._2)
   }

Work with S3 using a custom S3Client configuration
==================================================

**Motivation:** You would like to work with assets on S3, but you want
to use an S3 client (or clients) with a configuration (various
configurations) different from the default client configuration.

This can be accomplished by sub-classing ``S3AttributeStore`` and/or
``S3ValueReader``, perhaps anonymously.

.. code-block:: scala

   import geotrellis.spark.io.s3._
   import com.amazonaws.services.s3.{AmazonS3Client=>AWSAmazonS3Client}

   val aws: AWSAmazonS3Client = ??? /* Special configuration happens here */
   val specialS3client = new AmazonS3Client(aws)

   val attributeStore = new S3AttributeStore("my-bucket", "my-prefix") {
      override def s3Client = specialS3Client
   }

   val valueReader = new S3ValueReader(attributeStore) {
      override def s3Client = specialS3Client
   }

Saving the Tiles of a Layer as GeoTiffs to S3
===============================================

**Motivation:** You would like to save the ``Tile``\s of your layer as GeoTiffs
to a S3.

If the size of your ``Tile``\s are fine and you're to save them:

.. code-block:: scala

   import geotrellis.raster.io.geotiff._
   import geotrellis.spark.io.s3._

   val rdd: TileLayerRDD[SpatialKey] = ???

   // Convert the values of the layer to SinglebandGeoTiffs
   val geoTiffRDD: RDD[(K, SinglebandGeoTiff)] = rdd.toGeoTiffs()

   // Convert the GeoTiffs to Array[Byte]]
   val byteRDD: RDD[(K, Array[Byte])] = geoTiffRDD.mapValues { _.toByteArray }

   // In order to save files to S3, we need a function that converts the
   // Keys of the layer to URIs of their associated values.
   val keyToURI = (k: SpatialKey) => s"s3://path/to/geotiffs/${k.col}_${k.row}.tif"

   byteRDD.saveToS3(keyToURI)

If you'd like the size of the ``Tile``\s in the layer to be a different
size before saving:

.. code-block:: scala

   import geotrellis.raster.io.geotiff._
   import geotrellis.spark.io.s3._
   import geotrellis.spark.regrid._

   val rdd: TileLayerRDD[SpatialKey] = ???

   // Regrid the Tiles so that they are 512x512
   val regridedRDD: TileLayerRDD[SpatialKey] = rdd.regrid(512, 512)

   // Convert the values of the layer to SinglebandGeoTiffs
   val geoTiffRDD: RDD[(K, SinglebandGeoTiff)] = regridedRDD.toGeoTiffs()

   // Convert the GeoTiffs to Array[Byte]]
   val byteRDD: RDD[(K, Array[Byte])] = geoTiffRDD.mapValues { _.toByteArray }

   // In order to save files to S3, we need a function that converts the
   // Keys of the layer to URIs of their associated values.
   val keyToURI = (k: SpatialKey) => s"s3://path/to/geotiffs/${k.col}_${k.row}.tif"

   byteRDD.saveToS3(keyToURI)
