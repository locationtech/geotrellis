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
   * Functions that require RasterRDD to have a TMS grid dimension to their key
   */
  def up[K: SpatialComponent: ClassTag](rdd: RasterRDD[K, Tile], level: LayoutLevel, layoutScheme: LayoutScheme): (RasterRDD[K, Tile], LayoutLevel) = {
    val metadata = rdd.metaData
    val nextLevel = layoutScheme.zoomOut(level)
    val nextMetaData = 
      RasterMetaData(
        metadata.cellType,
        metadata.extent,
        metadata.crs,
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
          val extent = metadata.mapTransform(key)
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
            val oldExtent = metadata.mapTransform(oldKey)
            newTile.merge(newExtent, oldExtent, tile)
          }

          (newKey, newTile: Tile)
        }

    new TileRasterRDD(nextRdd, nextMetaData) -> nextLevel
  }
}
