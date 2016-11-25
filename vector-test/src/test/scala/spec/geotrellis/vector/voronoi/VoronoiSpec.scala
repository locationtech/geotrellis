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

package geotrellis.vector.voronoi

import geotrellis.vector._
import scala.util.Random
import scala.math.pow
import org.apache.commons.math3.linear._

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.render._

import org.scalatest.{FunSpec, Matchers}

class VoronoiSpec extends FunSpec with Matchers {

  def rasterizePoly(poly: Polygon, tile: MutableArrayTile, re: RasterExtent, erring: Boolean)(implicit trans: Int => Point) = {
    if (erring) {
      Rasterizer.foreachCellByPolygon(poly, re){ (c,r) => tile.set(c, r, 2) }
    } else {
      val l = poly.boundary.toGeometry.get
      Rasterizer.foreachCellByGeometry(l, re){ (c,r) => tile.set(c, r, 1) }
    }
  }

  def rasterizeVoronoi(voronoi: Voronoi)(implicit trans: Int => Point): Unit = {
    val tile = IntArrayTile.fill(255, 325, 600)
    val re = RasterExtent(Extent(-2.25, -3, 1, 3),325,600)
    voronoi.voronoiCells.foreach{ poly =>
      rasterizePoly(poly, tile, re, !poly.isValid)
    }
    val cm = ColorMap(scala.collection.immutable.Map(1 -> 0x000000ff, 2 -> 0xff0000ff, 255 -> 0xffffffff))
    tile.renderPng(cm).write("voronoi.png")
  }

  describe("Voronoi diagram") {
    it("should have valid polygons") {
      val pts = Array(Point(0,-2), Point(0,0), Point(0,1), Point(-0.5,2), Point(0.5,2))
      val voronoi = pts.voronoiDiagram()

      def validCoveredPolygon(polypt: (Polygon, Point)) = {
        val (poly, pt) = polypt
        val verts = poly.vertices
        verts.forall{ 
          v => pts.map(_ distance v).min >= v.distance(pt )
        }
      }

      voronoi.voronoiCellsWithPoints.forall (validCoveredPolygon(_)) should be (true)
      //rasterizeVoronoi(voronoi)
    }
  }
}
