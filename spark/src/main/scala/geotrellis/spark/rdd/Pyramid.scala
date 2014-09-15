package geotrellis.spark.rdd

import geotrellis.raster._
import geotrellis.spark.TmsTile
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._


object Pyramid {
  def levelUp(rdd: RasterRDD): RasterRDD = {
    val (nextRdd, nextMd) = levelUp(rdd, rdd.metaData)
    new RasterRDD(nextRdd, nextMd)
  }

  def levelUp(rdd: RDD[TmsTile], metaData: LayerMetaData): (RDD[TmsTile], LayerMetaData) = {
    val nextMetaData = metaData.copy(level = metaData.level.up)
    val nextRdd =
      rdd.map { case TmsTile(id, tile) =>
        val (x, y) = metaData.transform.indexToGrid(id)
        (x / 2, y / 2) ->(x % 2, y % 2, tile)
      }.combineByKey(
      { case (x: Int, y: Int, tile: Tile) =>
        val tiles: Array[Option[Tile]] = Array.fill(4)(None)
        tiles(y * 2 + x) = Some(tile)
        tiles
      }, { (tiles: Array[Option[Tile]], tup: (Int, Int, Tile)) =>
        val (x, y, tile) = tup
        tiles(y * 2 + x) = Some(tile)
        tiles
      }, { (tiles1: Array[Option[Tile]], tiles2: Array[Option[Tile]]) =>
        tiles1
          .zip(tiles2)
          .map { tup =>
          val tile: Option[Tile] = tup match {
            case (None, tile: Some[Tile]) => tile
            case (tile: Some[Tile], None) => tile
            case _ => sys.error("Indexing error when merging neighboring tiles")
          }
          tile
        }
      }
      ).map { case ((x, y), maybeTiles: Array[Option[Tile]]) =>
        val id = nextMetaData.transform.gridToIndex(x, y)

        val firstTile =  maybeTiles.flatten.apply(0)
        val (cols, rows) = firstTile.dimensions //Must be at least one tile
        val cellType = firstTile.cellType

        //Assuming that target extent in the new level will match the combined extent of 2x2 composite
        val targetExtent = nextMetaData.transform.gridToMap(x, y)
        val tile = CompositeTile(
          maybeTiles map { _.getOrElse(NoDataTile(cellType, cols, rows)) },
          TileLayout(2, 2, cols, rows))
        val warped = tile.warp(targetExtent, cols, rows)
        TmsTile(id, warped)
      }

    nextRdd -> nextMetaData
  }

}
