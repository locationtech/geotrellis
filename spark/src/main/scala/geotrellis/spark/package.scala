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

package geotrellis

import geotrellis.raster._
import geotrellis.vector._

import geotrellis.spark.formats._
import geotrellis.spark.tiling._
import geotrellis.spark.metadata.Context
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.rdd.SaveRasterFunctions

import org.apache.hadoop.fs.Path
import org.apache.spark.rdd._

import spire.syntax.cfor._

package object spark {
  implicit class MakeRasterRDD(val prev: RDD[TmsTile]) {
    def withContext(ctx: Context) = new RasterRDD(prev, ctx)
  }

  implicit class MakeRasterRDD2(val prev: RDD[(Long, Tile)]) {
    def withContext(ctx: Context) = new RasterRDD(prev, ctx)
  }

  implicit class SavableRasterRDD(val rdd: RasterRDD) {
    def save(path: Path) = SaveRasterFunctions.save(rdd, path)
    def save(path: String): Unit = save(new Path(path))
  }

  implicit def tmsTileRddToTupleRdd(rdd: RDD[TmsTile]): RDD[(Long, Tile)] =
    rdd.map { case TmsTile(id, tile) => (id, tile) }

  implicit def tupleRddToTmsTileRdd(rdd: RDD[(Long, Tile)]): RDD[TmsTile] =
    rdd.map { case (id, tile) => TmsTile(id, tile) }

  implicit def tmsTileRddToPairRddFunctions(rdd: RDD[TmsTile]): PairRDDFunctions[Long, Tile] =
    new PairRDDFunctions(tmsTileRddToTupleRdd(rdd))

  implicit def tmsTileRddToOrderedRddFunctions(rdd: RDD[TmsTile]): OrderedRDDFunctions[Long, Tile, (Long, Tile)] =
    new OrderedRDDFunctions(tmsTileRddToTupleRdd(rdd))

  implicit class ValueBurner(val tile: MutableArrayTile) {
    def burnValues(other: Tile): MutableArrayTile = {
      Seq(tile, other).assertEqualDimensions
      if(tile.cellType.isFloatingPoint) {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.getDouble(col, row))) {
              tile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      } else {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.get(col, row))) {
              tile.setDouble(col, row, other.get(col, row))
            }
          }
        }
      }

      tile
    }

    def burnValues(extent: Extent, otherExtent: Extent, other: Tile): MutableArrayTile =
      otherExtent & extent match {
        case PolygonResult(sharedExtent) =>
          val re = RasterExtent(extent, tile.cols, tile.rows)
          val GridBounds(colMin, colMax, rowMin, rowMax) = re.gridBoundsFor(sharedExtent)
          val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

          def thisToOther(col: Int, row: Int): (Int, Int) = {
            val (x, y) = re.gridToMap(col, row)
            otherRe.mapToGrid(x, y)
          }

          if(tile.cellType.isFloatingPoint) {
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.getDouble(col, row))) {
                  val (otherCol, otherRow) = thisToOther(col, row)
                  if(otherCol > 0 && otherCol < other.cols &&
                    otherRow > 0 && otherRow < other.rows)
                    tile.setDouble(col, row, other.getDouble(otherCol, otherRow))
                }
              }
            }
          } else {
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.get(col, row))) {
                  val (otherCol, otherRow) = thisToOther(col, row)
                  if(otherCol > 0 && otherCol < other.cols &&
                    otherRow > 0 && otherRow < other.rows)
                    tile.set(col, row, other.get(otherCol, otherRow))
                }
              }
            }

          }

          tile
        case _ =>
          tile
      }
  }
}
