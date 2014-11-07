package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._

import org.apache.spark.rdd._
import org.apache.spark.SparkContext._

import monocle._
import monocle.syntax._

import scala.reflect.ClassTag

object Pyramid {
  /** Save layers up */
  def saveLevels[K: SpatialComponent: ClassTag](layerMetaData: LayerMetaData, rdd: RasterRDD[K], layoutScheme: LayoutScheme)
                                               (save: (LayerMetaData, RasterRDD[K]) => Unit): Unit = {
    save(layerMetaData, rdd)
    if (layerMetaData.id.zoom > 1) Pyramid.up(rdd, layerMetaData.layoutLevel, layoutScheme)
  }

  /**
   * Functions that require RasterRDD to have a TMS grid dimension to their key
   */
  def up[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], level: LayoutLevel, layoutScheme: LayoutScheme): RasterRDD[K] = {
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
    def createTiles(tile: (K, Double, Double, Tile)): Seq[(K, Double, Double, Tile)] =
      Seq(tile)

    def mergeTiles1(tiles: Seq[(K, Double, Double, Tile)], tile: (K, Double, Double, Tile)): Seq[(K, Double, Double, Tile)] = 
      tiles :+ tile

    def mergeTiles2(tiles1: Seq[(K, Double, Double, Tile)], tiles2: Seq[(K, Double, Double, Tile)]): Seq[(K, Double, Double, Tile)] =
      tiles1 ++ tiles2
  
    val nextRdd: RDD[(K, Tile)] =
      rdd
        .map { case (key, tile: Tile) =>
          val extent = metaData.mapTransform(key)
          val newSpatialKey = metaData.mapTransform(extent.xmin, extent.ymax)
          (newSpatialKey, (key, extent.xmin, extent.ymax, tile))
         }
        .combineByKey(createTiles, mergeTiles1, mergeTiles2)
        .map { case (spatialKey: SpatialKey, seq: Seq[(K, Double, Double, Tile)]) =>
          val key = seq.head._1
          val orderedTiles = 
            seq
              .sortBy { case (_, x, y, _) => (x, -y) }
              .map { case (_, _, _, tile) => tile }

          val (xs, ys) =
            seq
              .foldLeft((Set[Double](),Set[Double]())) { (sets, tileTup) =>
                val (xs, ys) = sets
                val (_, x, y, _) = tileTup
                (xs + x, ys + y)
              }

          val (cols, rows) = (xs.size, ys.size)

          val tile = 
            CompositeTile(
              orderedTiles,
              TileLayout(cols, rows, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)
            )

          val newKey = key.updateSpatialComponent(spatialKey)
          val warped = tile.warp(nextMetaData.tileLayout.tileCols, nextMetaData.tileLayout.tileRows)

          (newKey, warped)
        }

    new RasterRDD(nextRdd, nextMetaData)
  }
}
