/***
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
 ***/

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

import scalaxy.loops._
import scala.collection.mutable

object Majority extends Serializable {
  def apply(rs:Op[Raster]*):Op[Raster] =
    apply(0,rs)

  def apply(rs:Seq[Op[Raster]])(implicit d:DI):Op[Raster] =
    apply(0,rs)

  def apply(level:Op[Int],rs:Op[Raster]*):Op[Raster] =
    apply(level,rs)

  def apply(level:Op[Int],rs:Seq[Op[Raster]])(implicit d:DI):Op[Raster] =
    (level,logic.Collect(rs)).map { (level,rs) =>
      if(Set(rs.map(_.rasterExtent)).size != 1) {
        val rasterExtents = rs.map(_.rasterExtent).toSeq
        throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
          s"$rasterExtents are not all equal")
      }

      val layerCount = rs.length
      if(layerCount == 0) {
        sys.error(s"Can't compute majority of empty sequence")
      } else {
        val newRasterType = rs.map(_.rasterType).reduce(_.union(_))
        val re = rs(0).rasterExtent
        val cols = re.cols
        val rows = re.rows
        val data = RasterData.allocByType(newRasterType,cols,rows)

        if(newRasterType.isDouble) {
          val counts = mutable.Map[Double,Int]()

          for(col <- 0 until cols) {
            for(row <- 0 until rows) {
              counts.clear
              for(r <- rs) {
                val v = r.getDouble(col,row)
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
              data.setDouble(col,row, m)
            }
          }
        } else {
          val counts = mutable.Map[Int,Int]()

          for(col <- 0 until cols) {
            for(row <- 0 until rows) {
              counts.clear
              for(r <- rs) {
                val v = r.get(col,row)
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
              data.set(col,row, m)
            }
          }
        }
        ArrayRaster(data,re)
      }
    }
    .withName("Majority")
}

trait MajorityOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rss:Seq[RasterSource]):RasterSource = 
    combineOp(rss)(Majority(_))

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rss:RasterSource*)(implicit d:DI):RasterSource = 
    localMajority(rss)

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n:Int,rss:Seq[RasterSource]):RasterSource = 
    combineOp(rss)(Majority(n,_))

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n:Int,rss:RasterSource*)(implicit d:DI):RasterSource = 
    localMajority(n,rss)
}
