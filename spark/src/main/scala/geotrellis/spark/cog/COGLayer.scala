package geotrellis.spark.cog

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.hadoop.fs.Path
import org.apache.spark.rdd._

import java.net.URI
import scala.reflect.ClassTag

object COGLayer {
  /**
    * Make it more generic? GeoTiffs are Iterables of (K, V)s // K - is a segment key, V - is a segment itself
    * Segments are in a row major order => profit?
    */
  def pyramidUp[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](itr: Iterable[(K, V)],
    endZoom: Int,
    layoutLevel: LayoutLevel,
    layoutScheme: LayoutScheme,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions
   )(implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): List[GeoTiff[V]] = {
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

      val ifdLayer: GeoTiff[V] =
        list.toGeoTiff(nextLayout, md, options.copy(subfileType = Some(ReducedImage)))

      ifdLayer :: pyramidUp(list, endZoom, nextLayoutLevel, layoutScheme, md, options)
    } else List()
  }

  def apply[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme)
   (implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): RDD[(K, GeoTiff[V])] = {
    val md = rdd.metadata
    val sourceLayout = md.layout
    val options: GeoTiffOptions = GeoTiffOptions(storageMethod = Tiled(sourceLayout.tileCols, sourceLayout.tileRows))
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
        // TODO: refactor, so ugly
        val list = partition.toList
        val flatList = list.flatMap(_._2)

        if(list.nonEmpty) {
          val sfc = list.head._1

          val overviews: List[GeoTiff[V]] =
            pyramidUp[K, V](flatList, endZoom, LayoutLevel(startZoom, sourceLayout), layoutScheme, md, options.copy(subfileType = Some(ReducedImage)))

          val stitchedTile: GeoTiff[V] =
            flatList.toGeoTiff(sourceLayout, md, options, overviews)

          Iterator(sfc -> stitchedTile)
        } else Iterator()
      }
  }

  def write[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](cogs: RDD[(K, GeoTiff[V])])(keyIndex: KeyIndex[K], uri: URI): Unit = {
    val conf = HadoopConfiguration(cogs.sparkContext.hadoopConfiguration)
    cogs.foreach { case (key, tiff) =>
      HdfsUtils.write(new Path(s"${uri.toString}/${keyIndex.toIndex(key)}.tiff"), conf.get) { new GeoTiffWriter(tiff, _).write(true) }
    }
  }
}
