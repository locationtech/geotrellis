package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.mosaic._

import org.apache.spark.Logging
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._

import monocle._
import monocle.syntax._

import scala.reflect.ClassTag

object Pyramid extends Logging {
  /**
   * Save layers up, until level 1 is reached
   * @param rdd           RDD containing original level to be pyramided
   * @param layoutScheme  LayoutScheme used to create the RDD
   * @param save          Function(rdd, layoutLevel) that will be called for zoom each level, including original
   */
  def saveLevels[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], level: LayoutLevel, layoutScheme: LayoutScheme)
                                               (save: (RasterRDD[K], LayoutLevel) => Unit): Unit = {
    logInfo(s"Saving raster at $level")
    save(rdd, level)
    if (level.zoom > 1) {
      val (nextRdd, nextLevel) = Pyramid.up(rdd, level, layoutScheme)
      saveLevels(nextRdd, nextLevel, layoutScheme)(save)
    }
  }

  /**
   * Functions that require RasterRDD to have a TMS grid dimension to their key
   */
  def up[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], level: LayoutLevel, layoutScheme: LayoutScheme): (RasterRDD[K], LayoutLevel) = {
    val metaData = rdd.metaData
    val nextLevel = layoutScheme.zoomOut(level)
    val nextMetaData = 
      RasterMetaData(
        metaData.cellType,
        metaData.extent,
        metaData.crs,
        nextLevel.tileLayout
      )

    // Functions for combine step
    def createTiles(tile: (K, Tile)): Seq[(K, Tile)] =
      Seq(tile)

    def mergeTiles1(tiles: Seq[(K, Tile)], tile: (K, Tile)): Seq[(K, Tile)] = 
      tiles :+ tile

    def mergeTiles2(tiles1: Seq[(K, Tile)], tiles2: Seq[(K, Tile)]): Seq[(K, Tile)] =
      tiles1 ++ tiles2
  
    val firstMap: RDD[(K, (K, Tile))] =
      rdd
        .map { case (key, tile: Tile) =>
          val extent = metaData.mapTransform(key)
          val newSpatialKey = nextMetaData.mapTransform(extent.center)
          (key.updateSpatialComponent(newSpatialKey), (key, tile))
         }

    val combined =
      firstMap
        .combineByKey(createTiles, mergeTiles1, mergeTiles2)

    val nextRdd: RDD[(K, Tile)] =
        combined.map { case (newKey: K, seq: Seq[(K, Tile)]) =>
          val key = seq.head._1

          val newExtent = nextMetaData.mapTransform(newKey)
          val newTile = ArrayTile.empty(nextMetaData.cellType, nextMetaData.tileLayout.tileCols, nextMetaData.tileLayout.tileRows)

          for( (oldKey, tile) <- seq) {
            val oldExtent = metaData.mapTransform(oldKey)
            newTile.merge(newExtent, oldExtent, tile)
          }

          (newKey, newTile: Tile)
        }

    new RasterRDD(nextRdd, nextMetaData) -> nextLevel
  }
}
