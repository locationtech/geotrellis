package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.mosaic._
import org.apache.spark.Logging
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object Pyramid extends Logging {

  def up[K: SpatialComponent: ClassTag, TileType : MergeView: CellGridPrototypeView](
    rdd: RDD[(K, TileType)], 
    sourceLayout: LayoutDefinition, 
    targetLayout: LayoutDefinition
  ): RDD[(K, TileType)] = {
  
    // Functions for combine step
    def createTiles(tile: (K, TileType)): Seq[(K, TileType)]                                    = Seq(tile)
    def mergeTiles1(tiles: Seq[(K, TileType)], tile: (K, TileType)): Seq[(K, TileType)]         = tiles :+ tile
    def mergeTiles2(tiles1: Seq[(K, TileType)], tiles2: Seq[(K, TileType)]): Seq[(K, TileType)] = tiles1 ++ tiles2

    val nextRdd =
      rdd
        .map { case (key, tile) =>
          val extent = sourceLayout.mapTransform(key)
          val newSpatialKey = targetLayout.mapTransform(extent.center)
          (key.updateSpatialComponent(newSpatialKey), (key, tile))
        }
        .combineByKey(createTiles, mergeTiles1, mergeTiles2)
        .map { case (newKey: K, seq: Seq[(K, TileType)]) =>
          val newExtent = targetLayout.mapTransform(newKey)
          val newTile = seq.head._2.prototype(targetLayout.tileLayout.tileCols, targetLayout.tileLayout.tileRows)

          for( (oldKey, tile) <- seq) {
            val oldExtent = sourceLayout.mapTransform(oldKey)
            newTile.merge(newExtent, oldExtent, tile)
          }
          (newKey, newTile: TileType)
        }

    nextRdd
  }

  def up[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], layoutScheme: LayoutScheme, zoom: Int): (Int, RasterRDD[K]) = {
    val LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(LayoutLevel(zoom, rdd.metaData.layout))
    val nextMetaData = RasterMetaData(
      rdd.metaData.cellType,
      nextLayout,
      rdd.metaData.layout.extent,
      rdd.metaData.crs
    )
    val nextRdd = up(rdd, rdd.metaData.layout, nextLayout)
    nextZoom -> new RasterRDD(nextRdd, nextMetaData)
  }

  def up[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K], layoutScheme: LayoutScheme, zoom: Int): (Int, MultiBandRasterRDD[K]) = {
    val LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(LayoutLevel(zoom, rdd.metaData.layout))
    val nextMetaData = RasterMetaData(
      rdd.metaData.cellType,
      nextLayout,
      rdd.metaData.layout.extent,
      rdd.metaData.crs
    )
    val nextRdd = up(rdd, rdd.metaData.layout, nextLayout)
    nextZoom -> new MultiBandRasterRDD(nextRdd, nextMetaData)
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
                                             (f: (MultiBandRasterRDD[K], Int) => MultiBandRasterRDD[K]): MultiBandRasterRDD[K] =
    upLevels(rdd, layoutScheme, startZoom, 0)(f)

  def upLevels[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K], layoutScheme: LayoutScheme, startZoom: Int, endZoom: Int)
                                             (f: (MultiBandRasterRDD[K], Int) => MultiBandRasterRDD[K]): MultiBandRasterRDD[K] = {
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
