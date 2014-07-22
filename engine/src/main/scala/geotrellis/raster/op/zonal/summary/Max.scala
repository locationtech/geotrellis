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

object Max extends TileSummary[Int, Int, ValueSource[Int]] {
  def handlePartialTile(pt: PartialTileIntersection): Int = {
    val PartialTileIntersection(r, rasterExtent, polygons) = pt
    var max = NODATA
    for(p <- polygons) {
      Rasterizer.foreachCellByFeature(p, rasterExtent)(
        new Callback {
          def apply(col: Int, row: Int) {
            val z = r.get(col, row)
            if (isData(z) && (z > max || isNoData(max)) ) { max = z }
          }
        }
      )
    }
    max
  }

  def handleFullTile(ft: FullTileIntersection): Int = {
    var max = NODATA
    ft.tile.foreach { (x: Int) =>
      if (isData(x) && (x > max || isNoData(max))) { max = x }
    }
    max
  }

  def converge(ds: DataSource[Int, _]) =
    ds.reduce { (a, b) => 
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.max(a, b) }
    }
}

object MaxDouble extends TileSummary[Double, Double, ValueSource[Double]] {
  def handlePartialTile(pt: PartialTileIntersection): Double = {
    val PartialTileIntersection(r, rasterExtent, polygons) = pt
    var max = Double.NaN
    for(p <- polygons) {
      Rasterizer.foreachCellByFeature(p, rasterExtent)(
        new Callback {
          def apply(col: Int, row: Int) {
            val z = r.getDouble(col, row)
            if (isData(z) && (z > max || isNoData(max))) { max = z }
          }
        }
      )
    }

    max
  }

  def handleFullTile(ft: FullTileIntersection): Double = {
    var max = Double.NaN
    ft.tile.foreach((x: Int) => if (isData(x) && (x > max || isNoData(max))) { max = x })
    max
  }

  def converge(ds: DataSource[Double, _]) =
    ds.reduce { (a, b) =>
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.max(a, b) }
    }
}
