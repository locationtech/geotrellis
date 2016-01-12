package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.mosaic._
import geotrellis.raster.prototype._
import org.apache.spark.Logging
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object Pyramid extends Logging {

  def up[K: SpatialComponent: ClassTag, V <: CellGrid: MergeView: (? => TilePrototypeMethods[V])](
    rdd: RDD[(K, V)], 
    sourceLayout: LayoutDefinition, 
    targetLayout: LayoutDefinition
  ): RDD[(K, V)] = {
  
    // Functions for combine step
    def createTiles(tile: (K, V)): Seq[(K, V)]                             = Seq(tile)
    def mergeTiles1(tiles: Seq[(K, V)], tile: (K, V)): Seq[(K, V)]         = tiles :+ tile
    def mergeTiles2(tiles1: Seq[(K, V)], tiles2: Seq[(K, V)]): Seq[(K, V)] = tiles1 ++ tiles2

    val nextRdd =
      rdd
        .map { case (key, tile) =>
          val extent = sourceLayout.mapTransform(key)
          val newSpatialKey = targetLayout.mapTransform(extent.center)
          (key.updateSpatialComponent(newSpatialKey), (key, tile))
        }
        .combineByKey(createTiles, mergeTiles1, mergeTiles2)
        .map { case (newKey: K, seq: Seq[(K, V)]) =>
          val newExtent = targetLayout.mapTransform(newKey)
          val newTile = seq.head._2.prototype(targetLayout.tileLayout.tileCols, targetLayout.tileLayout.tileRows)

          for( (oldKey, tile) <- seq) {
            val oldExtent = sourceLayout.mapTransform(oldKey)
            newTile.merge(newExtent, oldExtent, tile)
          }
          (newKey, newTile: V)
        }

    nextRdd
  }

  def up[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], layoutScheme: LayoutScheme, zoom: Int): (Int, RasterRDD[K]) = {
    val LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(LayoutLevel(zoom, rdd.metaData.layout))
    val nextMetaData = RasterMetaData(
      rdd.metaData.cellType,
      nextLayout,
      rdd.metaData.extent,
      rdd.metaData.crs
    )
    val nextRdd = up(rdd, rdd.metaData.layout, nextLayout)
    nextZoom -> new ContextRDD(nextRdd, nextMetaData)
  }

  def up[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K], layoutScheme: LayoutScheme, zoom: Int)(implicit d: DummyImplicit): (Int, MultiBandRasterRDD[K]) = {
    val LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(LayoutLevel(zoom, rdd.metadata.layout))
    val nextMetaData = RasterMetaData(
      rdd.metadata.cellType,
      nextLayout,
      rdd.metadata.extent,
      rdd.metadata.crs
    )
    val nextRdd = up(rdd, rdd.metadata.layout, nextLayout)
    nextZoom -> new ContextRDD(nextRdd, nextMetaData)
  }

  def upLevels[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], layoutScheme: LayoutScheme, startZoom: Int)
                                             (f: (RasterRDD[K], Int) => RasterRDD[K]): RasterRDD[K] =
    upLevels(rdd, layoutScheme, startZoom, 0)(f)

  def upLevels[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], layoutScheme: LayoutScheme, startZoom: Int, endZoom: Int)
                                             (f: (RasterRDD[K], Int) => RasterRDD[K]): RasterRDD[K] = {
    def runLevel(thisRdd: RasterRDD[K], thisZoom: Int): (RasterRDD[K], Int) =
      if (thisZoom > endZoom) {
        val (nextZoom, nextRdd) = Pyramid.up(f(thisRdd, thisZoom), layoutScheme, thisZoom)
        runLevel(nextRdd, nextZoom)
      } else {
        (f(thisRdd, thisZoom), thisZoom)
      }

    runLevel(rdd, startZoom)._1
  }

  def upLevels[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K], layoutScheme: LayoutScheme, startZoom: Int)
                                             (f: (MultiBandRasterRDD[K], Int) => MultiBandRasterRDD[K])(implicit d: DummyImplicit): MultiBandRasterRDD[K] =
    upLevels(rdd, layoutScheme, startZoom, 0)(f)

  def upLevels[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K], layoutScheme: LayoutScheme, startZoom: Int, endZoom: Int)
                                             (f: (MultiBandRasterRDD[K], Int) => MultiBandRasterRDD[K])(implicit d: DummyImplicit): MultiBandRasterRDD[K] = {
    def runLevel(thisRdd: MultiBandRasterRDD[K], thisZoom: Int): (MultiBandRasterRDD[K], Int) =
      if (thisZoom > endZoom) {
        val (nextZoom, nextRdd) = Pyramid.up(f(thisRdd, thisZoom), layoutScheme, thisZoom)
        runLevel(nextRdd, nextZoom)
      } else {
        (f(thisRdd, thisZoom), thisZoom)
      }

    runLevel(rdd, startZoom)._1
  }
}
