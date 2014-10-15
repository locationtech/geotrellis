package geotrellis.spark.op.transform

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._

import org.apache.spark.rdd._
import org.apache.spark.SparkContext._

import monocle._
import monocle.syntax._

import scala.reflect.ClassTag

object Pyramid {
  /** Trivial lens to allow us using SpatialKey keyed RDDs without extra effort */
  implicit val tileIdLens =  SimpleLens[SpatialKey, SpatialKey](x => x, (e, c) => c)

  /**
   * Functions that require RasterRDD to have a TMS grid dimension to their key
   */
  def up[K: ClassTag](rdd: RasterRDD[K], level: ZoomLevel, zoomScheme: ZoomScheme)
                     (implicit _id: SimpleLens[K, SpatialKey]): RasterRDD[K] = {
    val metaData = rdd.metaData
    val nextLevel = zoomScheme.zoomOut(level)
    val nextMetaData = 
      RasterMetaData(
        metaData.cellType,
        metaData.extent,
        metaData.crs,
        nextLevel.tileLayout
      )

    // Functions for combine step
    def createTiles(tup: (Int, Int, Tile)): Array[Option[Tile]] = {
      val (x, y, tile) = tup
      val tiles: Array[Option[Tile]] = Array.fill(4)(None)
      tiles(y * 2 + x) = Some(tile)
      tiles
    }

    def mergeTiles1(tiles: Array[Option[Tile]], tup: (Int, Int, Tile)): Array[Option[Tile]] = {
      val (x, y, tile) = tup
      tiles(y * 2 + x) = Some(tile)
      tiles
    }

    def mergeTiles2(tiles1: Array[Option[Tile]], tiles2: Array[Option[Tile]]): Array[Option[Tile]] = 
      tiles1
        .zip(tiles2)
        .map { tup =>
          val tile: Option[Tile] =
            tup match {
              case (None, tile: Some[Tile]) => tile
              case (tile: Some[Tile], None) => tile
              case (None, None) => None
              case _ => sys.error("Indexing error when merging neighboring tiles")
            }
          tile
        }
  
    val nextRdd =
      rdd
        .map { case (key, tile: Tile) =>
          val (x, y): (Int, Int) = metaData.transform.indexToGrid(key |-> _id get)
          val nextId = nextMetaData.transform.gridToIndex(x/2, y/2)
          val nextKey: K = key -> nextId
          (nextKey, (x % 2, y % 2, tile))
         }
        .combineByKey(createTiles, mergeTiles1, mergeTiles2)
        .map { case ((key: K, id: SpatialKey), maybeTiles: Array[Option[Tile]]) =>
          val firstTile = maybeTiles.flatten.apply(0)
          val (cols, rows) = firstTile.dimensions //Must be at least one tile
          val cellType = firstTile.cellType

          val tile = 
            CompositeTile(
              maybeTiles map { _.getOrElse(NoDataTile(cellType, cols, rows)) },
              TileLayout(2, 2, cols, rows)
            )

          //Assuming that target extent in the new level will match the combined extent of 2x2 composite
          val targetExtent = nextMetaData.transform.indexToMap(id)
          val warped = tile.warp(targetExtent, cols, rows)
          (key |-> _id set(id), warped)
        }

    new RasterRDD(nextRdd, nextMetaData)
  }
}
