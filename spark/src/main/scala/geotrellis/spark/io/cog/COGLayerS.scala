package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.spark.io.hadoop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.stitch.Stitcher
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.pyramid.Pyramid.{Options => PyramidOptions}
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.stitch.TileLayoutStitcher
import geotrellis.spark.util._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.hadoop.fs.Path
import org.apache.spark.rdd._

import java.net.URI

import scala.reflect.ClassTag

object COGLayerS {
  import geotrellis.spark.io.cog.COGLayer.ContextGeoTiff

  def pyramidUp[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ? => GeoTiffConstructMethods[V]
  ](itr: Iterable[(K, V)],
    endZoom: Int,
    layoutLevel: LayoutLevel,
    layoutScheme: LayoutScheme,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions
   ): List[GeoTiff[V]] = {
    val nextLayoutLevel @ LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(layoutLevel)
    if(nextZoom >= endZoom) {
      val list: List[(K, V)] =
        itr
          .map { case (key, tile) =>
            val extent: Extent = key.getComponent[SpatialKey].extent(layoutLevel.layout)
            val newSpatialKey = nextLayout.mapTransform(extent.center)
            (key.setComponent(newSpatialKey), (key, tile))
          }
          .groupBy(_._1)
          .map { case (newKey, (nseq: Seq[(K, (K, V))])) =>
            val seq = nseq.map(_._2)
            val newExtent = newKey.getComponent[SpatialKey].extent(nextLayout)
            val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

            for ((oldKey, tile) <- seq) {
              val oldExtent = oldKey.getComponent[SpatialKey].extent(layoutLevel.layout)
              newTile.merge(newExtent, oldExtent, tile, NearestNeighbor)
            }
            (newKey, newTile: V)
          }.toList

      val (stitchedTile: V, gb) =
        TileLayoutStitcher
          .stitch[V](list.map { case (k, v) => k.getComponent[SpatialKey] -> v })

      val ifdLayer: GeoTiff[V] =
        stitchedTile.toGeoTiff(nextLayout, gb, md, options.copy(subfileType = Some(ReducedImage)))

      ifdLayer :: pyramidUp(list, endZoom, nextLayoutLevel, layoutScheme, md, options)
    } else List()
  }

  /**
    * Make it more generic? GeoTiffs are Iterables of (K, V)s // K - is a segment key, V - is a segment itself
    * Segments are in a row major order => profit?
    */
  def pyramidUpWithMetadata[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ? => GeoTiffConstructMethods[V]
  ](itr: Iterable[(K, V)],
    endZoom: Int,
    layoutLevel: LayoutLevel,
    layoutScheme: ZoomedLayoutScheme,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions
   ): List[ContextGeoTiff[K, V]] = {
    val nextLayoutLevel @ LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(layoutLevel)
    if(nextZoom >= endZoom) {
      val list: List[(K, V)] =
        itr
          .map { case (key, tile) =>
            val extent: Extent = key.getComponent[SpatialKey].extent(layoutLevel.layout)
            val newSpatialKey = nextLayout.mapTransform(extent.center)
            (key.setComponent(newSpatialKey), (key, tile))
          }
          .groupBy(_._1)
          .map { case (newKey, (nseq: Seq[(K, (K, V))])) =>
            val seq = nseq.map(_._2)
            val newExtent = newKey.getComponent[SpatialKey].extent(nextLayout)
            val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

            for ((oldKey, tile) <- seq) {
              val oldExtent = oldKey.getComponent[SpatialKey].extent(layoutLevel.layout)
              newTile.merge(newExtent, oldExtent, tile, NearestNeighbor)
            }
            (newKey, newTile: V)
          }.toList

      val (stitchedTile: V, gb) =
        TileLayoutStitcher
          .stitch[V](list.map { case (k, v) => k.getComponent[SpatialKey] -> v })

      val ifdLayer: GeoTiff[V] =
        stitchedTile.toGeoTiff(nextLayout, gb, md, options.copy(subfileType = Some(ReducedImage)))

      val keys = list.map(_._1)
      println(s"keys: $keys")
      val nextMd = TileLayerMetadata[K](
        cellType = ifdLayer.cellType,
        layout = nextLayout,
        extent = ifdLayer.extent,
        crs = ifdLayer.crs,
        bounds = KeyBounds(keys.min, keys.max)
      )

      val ifdContextLayer = ContextGeoTiff(ifdLayer, nextMd, nextZoom, layoutScheme, None, Nil)

      ifdContextLayer :: pyramidUpWithMetadata(list, endZoom, nextLayoutLevel, layoutScheme, md, options)
    } else List()
  }

  def apply[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ? => GeoTiffConstructMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme): RDD[(K, GeoTiff[V])] = {
    val options: GeoTiffOptions = GeoTiffOptions(storageMethod = Tiled)

    val md = rdd.metadata
    val sourceLayout = md.layout
    val LayoutLevel(_, endLayout) = layoutScheme.zoomOut(LayoutLevel(endZoom + 1, sourceLayout))

    val groupedByEndZoom =
      rdd
        .map { case (key, tile) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val endSpatialKey = endLayout.mapTransform(extent.center)
          (key.setComponent(endSpatialKey), (key, tile))
        }
        .groupByKey()
        .cache()

    val groupedPartitions = groupedByEndZoom.count().toInt

    groupedByEndZoom
      .repartition(groupedPartitions)
      .mapPartitions { partition: Iterator[(K, (Iterable[(K, V)]))] =>
        val list = partition.toList
        val flatList = list.flatMap(_._2)

        if(list.nonEmpty) {
          val sfc = list.head._1

          val (stitchedTile, gb) = // replace to construct GeoTiff
            TileLayoutStitcher
              .stitch[V](flatList.map { case (k, v) => k.getComponent[SpatialKey] -> v })

          val baseLayer: GeoTiff[V] =
            stitchedTile
              .toGeoTiff(
                sourceLayout,
                gb,
                md,
                options,
                pyramidUp[K, V](flatList, endZoom, LayoutLevel(startZoom, sourceLayout), layoutScheme, md, options)
              )

          Iterator(sfc -> baseLayer)
        } else Iterator()
      }
  }

  def applyWithMetadata[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ? => GeoTiffConstructMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: ZoomedLayoutScheme): RDD[(K, ContextGeoTiff[K, V])] = {
    val md = rdd.metadata
    val sourceLayout = md.layout
    val options: GeoTiffOptions = GeoTiffOptions(/*compression = Deflate, */storageMethod = Tiled(sourceLayout.tileCols, sourceLayout.tileRows))
    val LayoutLevel(_, endLayout) = layoutScheme.zoomOut(LayoutLevel(endZoom + 1, sourceLayout))

    val groupedByEndZoom =
      rdd
        .map { case (key, tile) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val endSpatialKey = endLayout.mapTransform(extent.center)
          (key.setComponent(endSpatialKey), (key, tile))
        }
        .groupByKey()
        .cache()

    val groupedPartitions = groupedByEndZoom.count().toInt

    groupedByEndZoom
      .repartition(groupedPartitions)
      .mapPartitions { partition: Iterator[(K, (Iterable[(K, V)]))] =>
        // TODO: refactor, so ugly
        val list = partition.toList
        val flatList = list.flatMap(_._2)

        if(list.nonEmpty) {
          val sfc = list.head._1
          println(s"sfc: $sfc")

          val overviews: List[ContextGeoTiff[K, V]] =
            pyramidUpWithMetadata[K, V](flatList, endZoom, LayoutLevel(startZoom, sourceLayout), layoutScheme, md, options.copy(subfileType = Some(ReducedImage)))

          val (stitchedTile, gb) = // replace to construct GeoTiff
            TileLayoutStitcher
              .stitch[V](flatList.map { case (k, v) => k.getComponent[SpatialKey] -> v })

          val baseLayer: GeoTiff[V] =
            stitchedTile
              .toGeoTiff(
                sourceLayout,
                gb,
                md,
                options,
                pyramidUp[K, V](flatList, endZoom, LayoutLevel(startZoom, sourceLayout), layoutScheme, md, options)
              )

          /*val stitchedTile: GeoTiff[V] =
            flatList.toGeoTiff(sourceLayout, md, options, overviews.map(_.geoTiff))*/

          val keys = flatList.map(_._1)

          //val keys = list.map(_._1)
          val currentMd = TileLayerMetadata[K](
            cellType = baseLayer.cellType,
            layout = sourceLayout,
            extent = baseLayer.extent,
            crs = baseLayer.crs,
            bounds = md.bounds //KeyBounds(keys.min, keys.max)
          )

          /*println(s"overviews.map { o => o.zoom -> o.metadata }: ${overviews.map { o => o.zoom -> o.metadata }}")

          val ovrMetadata: List[(Int, TileLayerMetadata[K])] =
            transformKeyBounds(
              currentMd.bounds.asInstanceOf[KeyBounds[K]],
              LayoutLevel(startZoom, md.layout), endZoom, layoutScheme
            ).map { case (zoom, kb) =>
              zoom -> TileLayerMetadata[K](
                cellType = stitchedTile.cellType,
                layout = sourceLayout,
                extent = stitchedTile.extent,
                crs = stitchedTile.crs,
                bounds = kb
              )
            }*/

          val ifdContextLayer =
            ContextGeoTiff(
              baseLayer, currentMd, startZoom, layoutScheme,
              Some(endZoom -> startZoom), overviews.map { o => o.zoom -> o.metadata }
            )

          Iterator(sfc -> ifdContextLayer)
        } else Iterator()
      }

  }
}
