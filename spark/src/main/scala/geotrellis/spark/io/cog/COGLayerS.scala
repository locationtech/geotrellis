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

  def apply[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ? => GeoTiffConstructMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme): RDD[(K, GeoTiff[V])] = {
    val options: GeoTiffOptions = GeoTiffOptions(storageMethod = Tiled)

    val md = rdd.metadata
    val sourceLayout = md.layout
    val LayoutLevel(_, endLayout) = layoutScheme.zoomOut(LayoutLevel(endZoom, sourceLayout))

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
}
