/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.rasterize

import geotrellis.layers.Metadata
import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.vector._
import org.apache.spark.rdd._
import org.apache.spark.{HashPartitioner, Partitioner}
import org.apache.spark.rdd._

import scala.collection.immutable.VectorBuilder
import spire.syntax.cfor._


object RasterizeRDD {
  /**
   * Rasterize an RDD of Feature objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Feature data will be converted to type matching specified [[CellType]].
   * Feature rasterization order is undefined in this operation.
   *
   * @param cellType [[CellType]] for creating raster tiles
   * @param layout Raster layer layout for the result of rasterization
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def fromGeometry[G <: Geometry](
    geoms: RDD[G],
    value: Double,
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val features = geoms.map({ g => Feature(g, value) })
    fromFeature(features, cellType, layout, options, partitioner)
  }

  /**
   * Rasterize an RDD of Geometry objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Value will be converted to type matching specified [[CellType]].
   *
   * @param value Cell value for cells intersecting a geometry
   * @param layout Raster layer layout for the result of rasterization
   * @param cellType [[CellType]] for creating raster tiles
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def fromFeature[G <: Geometry](
    features: RDD[Feature[G, Double]],
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    // key the geometry to intersecting tiles so it can be rasterized in the map-side combine
    val keyed: RDD[(SpatialKey, (Feature[Geometry, Double], SpatialKey))] =
      features.flatMap { feature =>
        layout.mapTransform.keysForGeometry(feature.geom).toIterator
          .map(key => (key, (feature, key)) )
      }

    val createTile = (tup: (Feature[Geometry, Double], SpatialKey)) => {
      val (feature, key) = tup
      val tile = ArrayTile.empty(cellType, layout.tileCols, layout.tileRows)
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)
      feature.geom.foreach(re, options)({ (x, y) =>
        tile.setDouble(x, y, feature.data)
      })
      tile: MutableArrayTile
    }

    val updateTile = (
      tile: MutableArrayTile,
      tup: (Feature[Geometry, Double], SpatialKey)
    ) => {
      val (feature, key) = tup
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)

      feature.geom.foreach(re, options){
        tile.setDouble(_, _, feature.data)
      }
      tile: MutableArrayTile
    }

    val mergeTiles = (left: MutableArrayTile, right: MutableArrayTile) => {
      left.merge(right).mutable
    }

    val tiles: RDD[(SpatialKey, MutableArrayTile)] =
      keyed.combineByKeyWithClassTag[MutableArrayTile](
        createCombiner = createTile,
        mergeValue = updateTile,
        mergeCombiners = mergeTiles,
        partitioner.getOrElse(new HashPartitioner(features.getNumPartitions))
      )

    ContextRDD(tiles.asInstanceOf[RDD[(SpatialKey, Tile)]], layout)
  }

  /**
   * Rasterize an RDD of Feature objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Feature value will be converted to type matching specified [[CellType]].
   * Z-Index from [[CellValue]] will be maintained per-cell during rasterization.
   * A cell with greater zindex is always in front of a cell with a lower zinde.
   *
   * @param cellType [[CellType]] for creating raster tiles
   * @param layout Raster layer layout for the result of rasterization
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def fromFeatureWithZIndex[G <: Geometry](
    features: RDD[Feature[G, CellValue]],
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None,
    zIndexCellType: CellType = ByteConstantNoDataCellType
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {

    // key the geometry to intersecting tiles so it can be rasterized in the map-side combine
    val keyed: RDD[(SpatialKey, (Feature[Geometry, CellValue], SpatialKey))] =
      features.flatMap { feature =>
        layout.mapTransform.keysForGeometry(feature.geom).toIterator
          .map(key => (key, (feature, key)) )
      }

    val createTile = (tup: (Feature[Geometry, CellValue], SpatialKey)) => {
      val (feature, key) = tup
      val tile = ArrayTile.empty(cellType, layout.tileCols, layout.tileRows)
      val ztile = ArrayTile.empty(zIndexCellType, layout.tileCols, layout.tileRows)
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)

      feature.geom.foreach(re, options)({ (x: Int, y: Int) =>
        val priority = tup._1.data.zindex
        tile.setDouble(x, y, feature.data.value)
        ztile.setDouble(x, y, priority)
      })

      (tile, ztile): (MutableArrayTile, MutableArrayTile)
    }

    val updateTile = (
      pair: (MutableArrayTile, MutableArrayTile),
      tup: (Feature[Geometry, CellValue], SpatialKey)
    ) => {
      val (feature, key) = tup
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)
      val (tile, ztile) = pair
      val priority = tup._1.data.zindex
      feature.geom.foreach(re, options)({ (x: Int, y: Int) =>
        if (isNoData(pair._1.getDouble(x, y)) || (pair._2.get(x, y) < priority)) {
          tile.setDouble(x, y, feature.data.value)
          ztile.setDouble(x, y, priority)
        }
      })

      (tile, ztile): (MutableArrayTile, MutableArrayTile)
    }

    val mergeTiles = (pair1: (MutableArrayTile, MutableArrayTile), pair2: (MutableArrayTile, MutableArrayTile)) => {
      val (left, leftPriority) = pair1
      val (right, rightPriority) = pair2
      mergePriority(left, leftPriority, right, rightPriority)
        (left, leftPriority): (MutableArrayTile, MutableArrayTile)
    }

    val tiles: RDD[(SpatialKey, MutableArrayTile)] =
      keyed.combineByKeyWithClassTag[(MutableArrayTile, MutableArrayTile)](
        createCombiner = createTile,
        mergeValue = updateTile,
        mergeCombiners = mergeTiles,
        partitioner.getOrElse(new HashPartitioner(features.getNumPartitions))
      )
        .mapValues { tup  => tup._1 }

    ContextRDD(tiles.asInstanceOf[RDD[(SpatialKey, Tile)]], layout)
  }

  /** Merge two tiles respecting pixel-wise value priority */
  private[geotrellis]
  def mergePriority(
    leftTile: MutableArrayTile,
    leftPriority: MutableArrayTile,
    rightTile: MutableArrayTile,
    rightPriority: MutableArrayTile
  ): (MutableArrayTile, MutableArrayTile) = {
    Seq(leftTile, rightTile, leftPriority, rightPriority).assertEqualDimensions()

    leftTile.cellType match {
      case BitCellType =>
        cfor(0)(_ < leftTile.rows, _ + 1) { row =>
          cfor(0)(_ < leftTile.cols, _ + 1) { col =>
            val leftv = leftTile.get(col, row)
            val rightv = rightTile.get(col, row)
            val rightp = rightPriority.get(col, row)
            if (leftv == 0 && rightv == 1) { // merge seems to treat 0 as nodata
              leftTile.set(col, row, rightv)
              leftPriority.set(col, row, rightp)
            }
          }
        }
      case ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
        // Assume 0 as the transparent value
        cfor(0)(_ < leftTile.rows, _ + 1) { row =>
          cfor(0)(_ < leftTile.cols, _ + 1) { col =>
            val leftv = leftTile.get(col, row)
            val leftp = leftPriority.get(col, row)
            val rightv = rightTile.get(col, row)
            val rightp = rightPriority.get(col, row)
            if ((leftv == 0 && rightv != 0) || (leftv != 0 && rightv != 0 && leftp < rightp)) {
              leftTile.set(col, row, rightTile.get(col, row))
              leftPriority.set(col, row, rightp)
            }
          }
        }
      case FloatCellType | DoubleCellType =>
        // Assume 0.0 as the transparent value
        cfor(0)(_ < leftTile.rows, _ + 1) { row =>
          cfor(0)(_ < leftTile.cols, _ + 1) { col =>
            val leftv = leftTile.getDouble(col, row)
            val leftp = leftPriority.get(col, row)
            val rightv = rightTile.getDouble(col, row)
            val rightp = rightPriority.get(col, row)
            if ((leftv == 0.0 && rightv != 0.0) || (leftv != 0.0 && rightv != 0.0 && leftp < rightp)) {
              leftTile.setDouble(col, row, rightv)
              leftPriority.set(col, row, rightp)
            }
          }
        }
      case x if x.isFloatingPoint =>
        cfor(0)(_ < leftTile.rows, _ + 1) { row =>
          cfor(0)(_ < leftTile.cols, _ + 1) { col =>
            val leftv = leftTile.getDouble(col, row)
            val leftnd = isNoData(leftTile.getDouble(col, row))
            val leftp = leftPriority.get(col, row)
            val rightv = rightTile.getDouble(col, row)
            val rightnd = isNoData(rightTile.getDouble(col, row))
            val rightp = rightPriority.get(col, row)
            if ((leftnd && !rightnd) || (!leftnd && !rightnd && leftp < rightp)) {
              leftTile.setDouble(col, row, rightv)
              leftPriority.set(col, row, rightp)
            }
          }
        }
      case _ =>
        cfor(0)(_ < leftTile.rows, _ + 1) { row =>
          cfor(0)(_ < leftTile.cols, _ + 1) { col =>
            val leftv = leftTile.get(col, row)
            val leftnd = isNoData(leftTile.get(col, row))
            val leftp = leftPriority.get(col, row)
            val rightv = rightTile.get(col, row)
            val rightnd = isNoData(rightTile.get(col, row))
            val rightp = rightPriority.get(col, row)
            if ((leftnd && !rightnd) || (!leftnd && !rightnd && leftp < rightp)) {
              leftTile.set(col, row, rightv)
              leftPriority.set(col, row, rightp)
            }
          }
        }
    }

    (leftTile, leftPriority)
  }
}
