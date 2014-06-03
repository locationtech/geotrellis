/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

import scalaxy.loops._
import scala.collection.mutable

object Majority extends Serializable {
  def apply(r: Tile*): Tile = apply(0, r)

  def apply(rs: Seq[Tile])(implicit d: DI): Tile = apply(0, rs)

  def apply(level: Int, r: Tile*): Tile = apply(level, r)

  def apply(level: Int, rs: Seq[Tile])(implicit d: DI): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.length
    if(layerCount == 0) {
      sys.error(s"Can't compute majority of empty sequence")
    } else {
      val newCellType = rs.map(_.cellType).reduce(_.union(_))
      val (cols, rows) = rs(0).dimensions
      val tile = ArrayTile.alloc(newCellType, cols, rows)

      if(newCellType.isFloatingPoint) {
        val counts = mutable.Map[Double, Int]()

        for(col <- 0 until cols) {
          for(row <- 0 until rows) {
            counts.clear
            for(r <- rs) {
              val v = r.getDouble(col, row)
              if(isData(v)) {
                if(!counts.contains(v)) {
                  counts(v) = 1
                } else {
                  counts(v) += 1
                }
              }
            }

            val sorted =
              counts.keys
                .toSeq
                .sortBy { k => counts(k) }
                .toList
            val len = sorted.length - 1
            val m =
              if(len >= level) { sorted(len-level) }
              else { Double.NaN }
            tile.setDouble(col, row, m)
          }
        }
      } else {
        val counts = mutable.Map[Int, Int]()

        for(col <- 0 until cols) {
          for(row <- 0 until rows) {
            counts.clear
            for(r <- rs) {
              val v = r.get(col, row)
              if(isData(v)) {
                if(!counts.contains(v)) {
                  counts(v) = 1
                } else {
                  counts(v) += 1
                }
              }
            }

            val sorted =
              counts.keys
                .toSeq
                .sortBy { k => counts(k) }
                .toList
            val len = sorted.length - 1
            val m =
              if(len >= level) { sorted(len-level) }
              else { NODATA }
            tile.set(col, row, m)
          }
        }
      }
      tile
    }
  }
}

trait MajorityOpMethods[+Repr <: RasterSource] { self: Repr =>

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rss: Seq[RasterSource]): RasterSource =
    combine(rss, "Majority")(Majority(_))

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rss: RasterSource*)(implicit d: DI): RasterSource =
    localMajority(rss)

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rss: Seq[RasterSource]): RasterSource =
    combine(rss, "Majority")(Majority(n, _))

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rss: RasterSource*)(implicit d: DI): RasterSource =
    localMajority(n, rss)
}

trait MajorityMethods { self: Tile =>

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rs: Seq[Tile]): Tile =
    Majority(self +: rs)

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rs: Tile*)(implicit d: DI): Tile =
    localMajority(rs)

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rs: Seq[Tile]): Tile =
    Majority(n, self +: rs)

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rs: Tile*)(implicit d: DI): Tile =
    localMajority(n, rs)
}
