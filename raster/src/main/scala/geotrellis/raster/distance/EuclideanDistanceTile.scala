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

import org.locationtech.jts.geom.Coordinate

import geotrellis.raster._
import geotrellis.raster.rasterize.polygon.PolygonRasterizer
import geotrellis.vector.{Point, Polygon}
import geotrellis.vector.voronoi._

import scala.math.sqrt

object EuclideanDistanceTile {

  private def fillFn(rasterExtent: RasterExtent, tile: MutableArrayTile, base: Point)(col: Int, row: Int): Unit = {
    val (x,y) = rasterExtent.gridToMap(col, row)
    val currentValue = tile.getDouble(col, row)
    val newValue = sqrt((x - base.x) * (x - base.x) + (y - base.y) * (y - base.y))

    if (java.lang.Double.isNaN(currentValue) || currentValue > newValue)
      tile.setDouble(col, row, newValue)
  }

  def rasterizeDistanceCell(rasterExtent: RasterExtent, tile: MutableArrayTile)(arg: (Polygon, Coordinate)) = {
    val (poly, coord) = arg

    try {
      val buffered = poly.buffer(math.max(rasterExtent.cellwidth, rasterExtent.cellheight))
      PolygonRasterizer.foreachCellByPolygon(buffered, rasterExtent)(fillFn(rasterExtent, tile, Point.jtsCoord2Point(coord)))
    } catch {
      case e: Throwable => println(s"Error when handling ${poly}: ${e.getMessage}")
    }
  }

  def apply(pts: Array[Coordinate], rasterExtent: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType): Tile = {
    val vor = VoronoiDiagram(pts, rasterExtent.extent)
    val tile = ArrayTile.empty(cellType, rasterExtent.cols, rasterExtent.rows)

    vor.voronoiCellsWithPoints.foreach(rasterizeDistanceCell(rasterExtent, tile))
    tile
  }

  def apply(pts: Array[Point], rasterExtent: RasterExtent): Tile = {
    apply(pts.map{ pt => new Coordinate(pt.x, pt.y) }, rasterExtent)
  }

  def apply(pts: Array[(Double, Double)], rasterExtent: RasterExtent): Tile = {
    apply(pts.map{ case (x, y) => new Coordinate(x, y) }, rasterExtent)
  }

  def apply(pts: Array[(Double, Double, Double)], rasterExtent: RasterExtent): Tile = {
    apply(pts.map{ case (x, y, z) => new Coordinate(x, y, z) }, rasterExtent)
  }

}
