/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._

object Min extends TileSummary[Int,Int,ValueSource[Int]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):Int = {
    val PartialTileIntersection(r,polygons) = pt
    var min = NODATA
    for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (isData(z) && (z < min || isNoData(min)) ) { min = z }
          }
        }
      )
    }
    min
  }

  def handleFullTile(ft:FullTileIntersection):Int = {
    var min = NODATA
    ft.tile.foreach { (x:Int) =>
      if (isData(x) && (x < min || isNoData(min))) { min = x }
    }
    min
  }

  def converge(ds:DataSource[Int,_]) =
    ds.reduce { (a,b) => 
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.min(a,b) }
    }
}

object MinDouble extends TileSummary[Double,Double,ValueSource[Double]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):Double = {
    val PartialTileIntersection(r,polygons) = pt
    var min = Double.NaN
    for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (isData(z) && (z < min || isNoData(min))) { min = z }
          }
        }
      )
    }

    min
  }

  def handleFullTile(ft:FullTileIntersection):Double = {
    var min = Double.NaN
    ft.tile.foreach((x:Int) => if (isData(x) && (x < min || isNoData(min))) { min = x })
    min
  }

  def converge(ds:DataSource[Double,_]) =
    ds.reduce { (a,b) =>
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.min(a,b) }
    }
}
