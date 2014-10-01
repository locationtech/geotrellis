/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.rdd

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.ingest._ //TODO, this is a wrong place for .merge to live
import geotrellis.spark.tiling._

import geotrellis.spark.op.local._
import geotrellis.vector.Extent

import org.apache.spark._
import org.apache.spark.rdd._

import org.apache.spark.SparkContext._

import scala.reflect.ClassTag

import monocle.syntax._
import monocle._


object RasterRDD {
  /** Trivial lens to allow us using TileId keyed RDDs without extra effort */
  implicit val tileIdLens =  SimpleLens[TileId, TileId](x=>x, (e, c) => c)


  /**
   * Functions that require RasterRDD to have a TMS grid dimension to their key
   */
  implicit class TmsAddressableRasterRDD[K](rdd: RasterRDD[K])
                                           (implicit _id: SimpleLens[K, TileId])
  {

    def pyramidUp: RasterRDD[K] = {
      val metaData = rdd.metaData
      val nextMetaData = rdd.metaData.copy(level = rdd.metaData.level.up)

      val nextRdd =
      rdd
      .map {
        case (key, tile: Tile) =>
          val (x, y) = metaData.transform.indexToGrid(key |-> _id get)
          val nextId = nextMetaData.transform.gridToIndex(x/2, y/2)
          (key -> nextId) -> (x % 2, y % 2, tile)
      }
      .combineByKey(
        { case (x: Int, y: Int, tile: Tile) =>
            val tiles: Array[Option[Tile]] = Array.fill(4)(None)
            tiles(y * 2 + x) = Some(tile)
            tiles
        },
        { (tiles: Array[Option[Tile]], tup: (Int, Int, Tile)) =>
          val (x, y, tile) = tup
          tiles(y * 2 + x) = Some(tile)
          tiles
        },
        { (tiles1: Array[Option[Tile]], tiles2: Array[Option[Tile]]) =>
            tiles1.zip(tiles2).map { tup =>
              val tile: Option[Tile] = tup match {
                case (None, tile: Some[Tile]) => tile
                case (tile: Some[Tile], None) => tile
                case (None, None) => None
                case _ => sys.error("Indexing error when merging neighboring tiles")
              }
              tile
            }
        })
      .map { case ((key, id), maybeTiles: Array[Option[Tile]]) =>
        val firstTile = maybeTiles.flatten.apply(0)
        val (cols, rows) = firstTile.dimensions //Must be at least one tile
        val cellType = firstTile.cellType

        val tile = CompositeTile(
          maybeTiles map { _.getOrElse(NoDataTile(cellType, cols, rows)) },
          TileLayout(2, 2, cols, rows))

        //Assuming that target extent in the new level will match the combined extent of 2x2 composite
        val targetExtent = nextMetaData.transform.indexToMap(id)
        val warped = tile.warp(targetExtent, cols, rows)
        (key |-> _id set(id), warped)
      }

      new RasterRDD(nextRdd, nextMetaData)
    }
  }
}

class RasterRDD[K](val prev: RDD[(K, Tile)], val metaData: LayerMetaData) extends RDD[(K, Tile)](prev) {
  override val partitioner = prev.partitioner

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

  /**
   * Given a function that provides extent information for every tile in the sequence we will
   * split/merge the source as needed to produce a RasterRDD with Tiles divided according to
   * the tiling scheme specified in the metaData.
   *
   * @param extentOf function that extracts extent information from the key
   * @param toKey    function that maps to the new RDD key, allowing you to add TileId information
   * @tparam KT      key type of the resulting RDD
   */
  def mosaic[KT : ClassTag](extentOf: K => Extent, toKey: (K, TileId) => KT): RasterRDD[KT] = {
    val bcMetaData = sparkContext.broadcast(metaData)
    val newRdd = this
      .flatMap { case (key, tile) =>
      val metaData = bcMetaData.value
      val extent = extentOf(key)
      metaData.transform.mapToGrid(extent).coords.map { coord =>
        val tileId = metaData.transform.gridToIndex(coord)
        val kt = toKey(key, tileId) //convert into new key, using the helpful function
        (kt, (tileId, extent, tile))
      }
    }
      .combineByKey(
    {case (id, extent, tile) =>
      val metaData = bcMetaData.value
      val tmsTile = ArrayTile.empty(metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
      tmsTile.merge(metaData.transform.indexToMap(id), metaData.extent, tile)
    }, {
      (tmsTile: MutableArrayTile, tup: (TileId, Extent, Tile)) =>
        val metaData = bcMetaData.value
        val (id, extent, tile) = tup
        tmsTile.merge(metaData.transform.indexToMap(id), extent, tile)
    }, {
      (tmsTile1: MutableArrayTile, tmsTile2: MutableArrayTile) =>
        tmsTile1.merge(tmsTile2)
    }
    )
      .map{ case (key, tile) =>
      (key, tile.asInstanceOf[Tile])
    }
    new RasterRDD[KT](newRdd, metaData)
  }

  def mapTiles(f: ((K, Tile)) => (K, Tile)): RasterRDD[K] =
    asRasterRDD(metaData) {
      mapPartitions({ partition =>
        partition.map { tile =>
          f(tile)
        }
      }, true)
    }

  def combineTiles[R](other: RasterRDD[K])(f: ((K, Tile), (K, Tile)) => (R, Tile)): RasterRDD[R] =
    asRasterRDD(metaData) {
      zipPartitions(other, true) { (partition1, partition2) =>
        partition1.zip(partition2).map {
          case (tile1, tile2) =>
            f(tile1, tile2)
        }
      }
    }

//  def minMax: (Int, Int) =
//    map(_.tile.findMinMax)
//      .reduce { (t1, t2) =>
//      val (min1, max1) = t1
//      val (min2, max2) = t2
//      val min =
//        if(isNoData(min1)) min2
//        else {
//          if(isNoData(min2)) min1
//          else math.min(min1, min2)
//        }
//      val max =
//        if(isNoData(max1)) max2
//        else {
//          if(isNoData(max2)) max1
//          else math.max(max1, max2)
//        }
//      (min, max)
//    }
}
