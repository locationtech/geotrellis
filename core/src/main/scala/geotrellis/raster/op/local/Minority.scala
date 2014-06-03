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

object Minority extends Serializable {
  def apply(r: Tile*): Tile =
    apply(0, r)

  def apply(rs: Seq[Tile])(implicit d: DI): Tile =
    apply(0, rs)

  def apply(level: Int, rs: Tile*): Tile =
    apply(level, rs)

  def apply(level: Int, rs: Seq[Tile])(implicit d: DI): Tile = {
    // TODO: Replace all of these with rs.assertEqualDimensions
    if(Set(rs.map(_.dimensions)).size != 1) {
      val dimensions = rs.map(_.dimensions).toSeq
      throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
        s"$dimensions are not all equal")
    }

    val layerCount = rs.length
    if(layerCount == 0) {
      sys.error(s"Can't compute minority of empty sequence")
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
                .sortBy { k => -counts(k) }
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
                .sortBy { k => -counts(k) }
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

trait MinorityOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rss: Seq[RasterSource]): RasterSource = 
    combine(rss, "Minority")(Minority(_))

  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rss: RasterSource*)(implicit d: DI): RasterSource = 
    localMinority(rss)

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n: Int, rss: Seq[RasterSource]): RasterSource = 
    combine(rss, "Minority")(Minority(n, _))

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n: Int, rss: RasterSource*)(implicit d: DI): RasterSource = 
    localMinority(n, rss)
}

trait MinorityMethods { self: Tile =>
  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rs: Seq[Tile]): Tile =
    Minority(self +: rs)

  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rs: Tile*)(implicit d: DI): Tile =
    localMinority(rs)

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n: Int, rs: Seq[Tile]): Tile =
    Minority(n, self +: rs)

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n: Int, rs: Tile*)(implicit d: DI): Tile =
    localMinority(n, rs)
}
