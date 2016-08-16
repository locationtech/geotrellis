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
    val re = RasterExtent(voronoi.extent,325,600)
    voronoi.voronoiCells.foreach{ poly =>
      rasterizePoly(poly, tile, re, !(poly.isValid && voronoi.extent.covers(poly)))
    }
    val cm = ColorMap(scala.collection.immutable.Map(1 -> 0x000000ff, 2 -> 0xff0000ff, 255 -> 0xffffffff))
    tile.renderPng(cm).write("voronoi.png")
  }

  describe("Voronoi diagram") {
    it("should have valid polygons entirely covered by the extent") {
      val extent = Extent(-2.25, -3, 1, 3)
      val pts = Array(Point(0,-2), Point(0,0), Point(0,1), Point(-0.5,2), Point(0.5,2))
      implicit val trans = { i: Int => pts(i) }
      val voronoi = pts.voronoiDiagram(extent)

      def validCoveredPolygon(poly: Polygon) = {
        poly.isValid && extent.covers(poly)
      }

      voronoi.voronoiCells.forall (validCoveredPolygon(_)) should be (true)
      //rasterizeVoronoi(voronoi)
    }
  }
}
