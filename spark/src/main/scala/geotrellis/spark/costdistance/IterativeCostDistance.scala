/*
 * Copyright 2017 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.costdistance

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.costdistance.SimpleCostDistance
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector._
import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.AccumulatorV2

import scala.collection.mutable


/**
  * This Spark-enabled implementation of the standard cost-distance
  * algorithm mentioned in the "previous work" section of [1] is
  * "heavily inspired" by the MrGeo implementation [2] but does not
  * share any code with it.
  *
  * 1. Tomlin, Dana.
  *    "Propagating radial waves of travel cost in a grid."
  *    International Journal of Geographical Information Science 24.9 (2010): 1391-1413.
  *
  * 2. https://github.com/ngageoint/mrgeo/blob/0c6ed4a7e66bb0923ec5c570b102862aee9e885e/mrgeo-mapalgebra/mrgeo-mapalgebra-costdistance/src/main/scala/org/mrgeo/mapalgebra/CostDistanceMapOp.scala
  */
object IterativeCostDistance {

  type KeyCostPair = (SpatialKey, SimpleCostDistance.Cost)
  type Changes = mutable.ArrayBuffer[KeyCostPair]

  val logger = Logger.getLogger(IterativeCostDistance.getClass)

  /**
    * An accumulator to hold lists of edge changes.
    */
  class ChangesAccumulator extends AccumulatorV2[KeyCostPair, Changes] {
    private val list: Changes = mutable.ArrayBuffer.empty

    def copy: ChangesAccumulator = {
      val other = new ChangesAccumulator
      other.merge(this)
      other
    }
    def add(pair: KeyCostPair): Unit = {
      this.synchronized { list += pair }
    }
    def isZero: Boolean = list.isEmpty
    def merge(other: AccumulatorV2[KeyCostPair, Changes]): Unit =
      this.synchronized { list ++= other.value }
    def reset: Unit = this.synchronized { list.clear }
    def value: Changes = list
  }

  def computeResolution[K: (? => SpatialKey), V: (? => Tile)](
    friction: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]
  ) = {
    val md = friction.metadata
    val mt = md.mapTransform
    val key: SpatialKey = md.bounds.get.minKey
    val extent = mt(key).reproject(md.crs, LatLng)
    val degrees = extent.xmax - extent.xmin
    val meters = degrees * (6378137 * 2.0 * math.Pi) / 360.0
    val pixels = md.layout.tileCols
    math.abs(meters / pixels)
  }

  private def geometryToKeys[K: (? => SpatialKey)](
    md: TileLayerMetadata[K],
    g: Geometry
  ) = {
    val keys = mutable.ArrayBuffer.empty[SpatialKey]
    val bounds = md.layout.mapTransform(g.envelope)

    var row = bounds.rowMin; while (row <= bounds.rowMax) {
      var col = bounds.colMin; while (col <= bounds.colMax) {
        keys += SpatialKey(col, row)
        col += 1
      }
      row += 1
    }

    keys.toList
  }

  private def geometryMap[K: (? => SpatialKey)](
    md: TileLayerMetadata[K],
    gs: Seq[Geometry]
  ): Map[SpatialKey, Seq[Geometry]] = {
    gs
      .flatMap({ g => geometryToKeys(md, g).map({ k => (k, g) }) })
      .groupBy(_._1)
      .map({ case (key, list) => (key, list.map({ case (_, v) => v })) })
  }

  /**
    * Perform the cost-distance computation.
    *
    * @param  friction    The friction layer; pixels are in units of "seconds per meter"
    * @param  geometries  The starting locations from-which to compute the cost of traveling
    * @param  maxCost     The maximum cost before pruning a path (in units of "seconds")
    */
  def apply[K: (? => SpatialKey), V: (? => Tile)](
    friction: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    geometries: Seq[Geometry],
    maxCost: Double = Double.PositiveInfinity
  ): RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]]= {

    val sparkContext = friction.sparkContext

    val md = friction.metadata
    val mt = md.mapTransform
    val resolution = computeResolution(friction)
    logger.debug(s"Computed resolution: $resolution meters/pixel")

    val bounds = friction.metadata.bounds match {
      case b: KeyBounds[K] => b
      case _ => throw new Exception
    }
    val minKey: SpatialKey = bounds.minKey
    val minKeyCol = minKey.col
    val minKeyRow = minKey.row
    val maxKey: SpatialKey = bounds.maxKey
    val maxKeyCol = maxKey.col
    val maxKeyRow = maxKey.row

    val accumulator = new ChangesAccumulator
    sparkContext.register(accumulator)

    // Index the input geometry by SpatialKey
    val gs = sparkContext.broadcast(geometryMap(md, geometries))

    // Create RDD of initial (empty) cost tiles and load the
    // accumulator with the starting values.
    var costs: RDD[(K, V, DoubleArrayTile)] = friction.map({ case (k, v) =>
      val key: SpatialKey = k
      val tile: Tile = v
      val cols = tile.cols
      val rows = tile.rows
      val extent = mt(key)
      val rasterExtent = RasterExtent(extent, cols, rows)
      val options = Rasterizer.Options.DEFAULT

      gs.value.getOrElse(key, List.empty[Geometry])
        .filter({ geometry => extent.intersects(geometry) })
        .foreach({ geometry =>
          Rasterizer
            .foreachCellByGeometry(geometry, rasterExtent, options)({ (col, row) =>
              val friction = tile.getDouble(col, row)
              val entry = (col, row, friction, 0.0)
              accumulator.add((key, entry))
            })
        })

      (k, v, SimpleCostDistance.generateEmptyCostTile(cols, rows))
    }).persist(StorageLevel.MEMORY_AND_DISK_SER)

    costs.count

    // Repeatedly map over the RDD of cost tiles until no more changes
    // occur on the periphery of any tile.
    do {
      val _changes: Map[SpatialKey, Seq[SimpleCostDistance.Cost]] =
        accumulator.value
          .groupBy(_._1)
          .map({ case (k, list) => (k, list.map({ case (_, v) => v })) })
      val changes = sparkContext.broadcast(_changes)
      logger.debug(s"At least ${changes.value.size} changed tiles")

      accumulator.reset

      val previous = costs

      costs = previous.map({ case (k, v, oldCostTile) =>
        val key: SpatialKey = k
        val frictionTile: Tile = v
        val keyCol = key.col
        val keyRow = key.row
        val frictionTileCols = frictionTile.cols
        val frictionTileRows = frictionTile.rows
        val localChanges: Option[Seq[SimpleCostDistance.Cost]] = changes.value.get(key)

        localChanges match {
          case Some(localChanges) => {
            val q: SimpleCostDistance.Q = {
              val q = SimpleCostDistance.generateEmptyQueue(frictionTileCols, frictionTileRows)
              localChanges.foreach({ (entry: SimpleCostDistance.Cost) => q.add(entry) })
              q
            }

            val newCostTile = SimpleCostDistance.compute(
              frictionTile, oldCostTile,
              maxCost, resolution,
              q, { (entry: SimpleCostDistance.Cost) =>
                val (col, row, f, c) = entry
                if (col == 0 && (minKeyCol <= keyCol-1)) // left
                  accumulator.add((SpatialKey(keyCol-1, keyRow), (frictionTileCols, row, f, c)))

                if (row == frictionTileRows-1 && (keyRow+1 <= maxKeyRow)) // up
                  accumulator.add((SpatialKey(keyCol, keyRow+1), (col, -1, f, c)))

                if (col == frictionTileCols-1 && (keyCol+1 <= maxKeyCol)) // right
                  accumulator.add((SpatialKey(keyCol+1, keyRow), (-1, row, f, c)))

                if (row == 0 && (minKeyRow <= keyRow-1)) // down
                  accumulator.add((SpatialKey(keyCol, keyRow-1), (col, frictionTileRows, f, c)))

                if (col == 0 && row == 0 && (minKeyCol <= keyCol-1) && (minKeyRow <= keyRow-1)) // upper-left
                  accumulator.add((SpatialKey(keyCol-1,keyRow-1), (frictionTileCols, frictionTileRows, f, c)))

                if (col == frictionTileCols-1 && row == 0 && (keyCol+1 <= maxKeyCol) && (minKeyRow <= keyRow-1)) // upper-right
                  accumulator.add((SpatialKey(keyCol+1,keyRow-1), (-1, frictionTileRows, f, c)))

                if (col == frictionTileCols-1 && row == frictionTileRows-1 && (keyCol+1 <= maxKeyCol) && (keyRow+1 <= maxKeyCol)) // lower-right
                  accumulator.add((SpatialKey(keyCol+1,keyRow+1), (-1, -1, f, c)))

                if (col == 0 && row == frictionTileRows-1 && (minKeyCol <= keyCol-1) && (keyRow+1 <= maxKeyRow)) // lower-left
                  accumulator.add((SpatialKey(keyCol-1,keyRow+1), (frictionTileCols, -1, f, c)))
              })

            (k, v, newCostTile)
          }
          case None => (k, v, oldCostTile)
        }
      }).persist(StorageLevel.MEMORY_AND_DISK_SER)

      costs.count
      previous.unpersist()
    } while (accumulator.value.nonEmpty)

    // Construct return value and return it
    val metadata = TileLayerMetadata(DoubleCellType, md.layout, md.extent, md.crs, md.bounds)
    val rdd = costs.map({ case (k, _, cost) => (k, cost: Tile) })
    ContextRDD(rdd, metadata)
  }

}
