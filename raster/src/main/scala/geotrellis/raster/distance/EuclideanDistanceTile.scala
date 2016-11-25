/*
 * Copyright 2016 Azavea
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

package geotrellis.raster.distance

import geotrellis.raster.{RasterExtent, DoubleArrayTile, Tile}
import geotrellis.raster.rasterize.polygon.PolygonRasterizer
import geotrellis.vector.{Point, Polygon}
import geotrellis.vector.voronoi._
import scala.math.sqrt

object EuclideanDistanceTile {

  def apply(pts: Array[Point], rasterExtent: RasterExtent): Tile = {
    val vor = pts.toList.voronoiDiagram()
    val tile = DoubleArrayTile.empty(rasterExtent.cols, rasterExtent.rows)
    
    def fillFn(base: Point)(col: Int, row: Int): Unit = {
      val (x,y) = rasterExtent.gridToMap(col, row)
      tile.setDouble(col, row, sqrt((x - base.x) * (x - base.x) + (y - base.y) * (y - base.y)))
    }

    def rasterizeDistanceCell(arg: (Polygon, Point)) = {
      val (poly, pt) = arg
      
      PolygonRasterizer.foreachCellByPolygon(poly, rasterExtent)(fillFn(pt))
    }
    vor.voronoiCellsWithPoints.foreach(rasterizeDistanceCell)

    tile
  }

}
