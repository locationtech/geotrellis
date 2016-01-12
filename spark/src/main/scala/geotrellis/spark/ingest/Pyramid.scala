package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import org.apache.spark.Logging
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object Pyramid extends Logging {

  def up[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[RasterMetaData], layoutScheme: LayoutScheme, zoom: Int): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) = {
    val LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(LayoutLevel(zoom, rdd.metaData.layout))
    val nextMetaData = RasterMetaData(
      rdd.metadata.cellType,
      nextLayout,
      rdd.metadata.extent,
      rdd.metadata.crs
    )
    // Functions for combine step
    def createTiles(tile: (K, V)): Seq[(K, V)]                             = Seq(tile)
    def mergeTiles1(tiles: Seq[(K, V)], tile: (K, V)): Seq[(K, V)]         = tiles :+ tile
    def mergeTiles2(tiles1: Seq[(K, V)], tiles2: Seq[(K, V)]): Seq[(K, V)] = tiles1 ++ tiles2

    val sourceLayout = rdd.metadata.layout

    val nextRdd =
      rdd
        .map { case (key, tile) =>
          val extent = sourceLayout.mapTransform(key)
          val newSpatialKey = nextLayout.mapTransform(extent.center)
          (key.updateSpatialComponent(newSpatialKey), (key, tile))
        }
        .combineByKey(createTiles, mergeTiles1, mergeTiles2)
        .map { case (newKey: K, seq: Seq[(K, V)]) =>
          val newExtent = nextLayout.mapTransform(newKey)
          val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

          for( (oldKey, tile) <- seq) {
            val oldExtent = sourceLayout.mapTransform(oldKey)
            newTile.merge(newExtent, oldExtent, tile)
          }
          (newKey, newTile: V)
        }

    nextZoom -> new ContextRDD(nextRdd, nextMetaData)
  }

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[RasterMetaData], layoutScheme: LayoutScheme, startZoom: Int)
   (f: (RDD[(K, V)] with Metadata[RasterMetaData], Int) => Unit): RDD[(K, V)] with Metadata[RasterMetaData] =
    upLevels(rdd, layoutScheme, startZoom, 0)(f)

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[RasterMetaData], layoutScheme: LayoutScheme, startZoom: Int, endZoom: Int)
   (f: (RDD[(K, V)] with Metadata[RasterMetaData], Int) => Unit): RDD[(K, V)] with Metadata[RasterMetaData] = {
    def runLevel(thisRdd: RDD[(K, V)] with Metadata[RasterMetaData], thisZoom: Int): (RDD[(K, V)] with Metadata[RasterMetaData], Int) =
      if (thisZoom > endZoom) {
        f(thisRdd, thisZoom)
        val (nextZoom, nextRdd) = Pyramid.up(thisRdd, layoutScheme, thisZoom)
        runLevel(nextRdd, nextZoom)
      } else {
        f(thisRdd, thisZoom)
        (thisRdd, thisZoom)
      }

    runLevel(rdd, startZoom)._1
  }
}
