Example Archive
***************

This is a collection of code snippets that represent common GeoTrellis tasks.

Get the ``Extent`` of a ``Tile``
================================

**Motivation:** Given a pair ``(SpatialKey, Tile)``, you'd like to know the
geographic area on the earth that they cover (the ``Extent``). This ``Extent``
has many uses within GeoTrellis.

This solution assumes you have a ``LayoutDefinition`` on hand. If you're working
with a ``TileLayerRDD``, as many GeoTrellis operations do, then grabbing one is just:

.. code:: scala

   val layer: TileLayerRDD[SpatialKey] = ...  /* Output of previous work */

   val layout: LayoutDefinition = layer.metadata.layout

Now to find the ``Extent`` of the key/tile pair:

.. code:: scala

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

.. code:: scala

   val tile: Tile = ...
   val crs: CRS = ...
   val extent: Extent = ...

   val raster: ProjectedRaster[Tile] = ProjectedRaster(Raster(tile, extent), crs)

This is the minimum amount of information required to construct a Layer. A function
that does that could look like this:

.. code:: scala

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
