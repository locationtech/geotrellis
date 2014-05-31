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

/**
 * Variety gives the count of unique values at each location in a set of Rasters.
 * 
 * @return     An TypeInt raster with the count values.
 */
object Variety extends Serializable {
  def apply(r: Raster*)(implicit d: DI): Raster =
    apply(r)
  def apply(rs: Seq[Raster]): Raster = {
    if(Set(rs.map(_.dimensions)).size != 1) {
      val dimensions = rs.map(_.dimensions).toSeq
      throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
        s"$dimensions are not all equal")
    }

    val layerCount = rs.length
    if(layerCount == 0) {
      sys.error(s"Can't compute variety of empty sequence")
    } else {
      val (cols, rows) = rs(0).dimensions
      val data = ArrayTile.allocByType(TypeInt, cols, rows)

      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          val variety =
            rs.map(r => r.get(col, row))
              .toSet
              .filter(isData(_))
              .size
          data.set(col, row, if(variety == 0) { NODATA } else { variety })
        }
      }
      Raster(data, cols, rows)
    }
  }
}
