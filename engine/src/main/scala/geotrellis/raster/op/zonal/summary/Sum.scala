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

package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.engine._

object Sum extends TileSummary[Long, Long, ValueSource[Long]] {
  def handlePartialTile(pt: PartialTileIntersection): Long = {
    val PartialTileIntersection(r, rasterExtent, polygons) = pt
    var sum: Long = 0L
    for(p <- polygons) {
      Rasterizer.foreachCellByFeature(p, rasterExtent)(
        new Callback {
          def apply(col: Int, row: Int) {
            val z = r.get(col, row)
            if (isData(z)) { sum = sum + z }
          }
        }
      )
    }

    sum
  }

  def handleFullTile(ft: FullTileIntersection): Long = {
    var s = 0L
    ft.tile.foreach((x: Int) => if (isData(x)) s = s + x)
    s
  }

  def converge(ds: DataSource[Long, _]) =
    ds.reduce(_+_)
}

object SumDouble extends TileSummary[Double, Double, ValueSource[Double]] {
  def handlePartialTile(pt: PartialTileIntersection): Double = {
    val PartialTileIntersection(r, rasterExtent, polygons) = pt
    var sum = 0.0
    for(p <- polygons) {
      Rasterizer.foreachCellByFeature(p, rasterExtent)(
        new Callback {
          def apply(col: Int, row: Int) {
            val z = r.getDouble(col, row)
            if(isData(z)) { sum = sum + z }
          }
        }
      )
    }

    sum
  }

  def handleFullTile(ft: FullTileIntersection): Double = {
    var s = 0.0
    ft.tile.foreachDouble((x: Double) => if (isData(x)) s = s + x)
    s
  }

  def converge(ds: DataSource[Double, _]) =
    ds.reduce(_+_)
}
