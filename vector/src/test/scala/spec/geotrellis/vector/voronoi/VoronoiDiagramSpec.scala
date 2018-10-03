package geotrellis.vector.voronoi

import org.locationtech.jts.geom.Coordinate
import org.apache.commons.math3.linear._

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.render._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._
import geotrellis.vector.triangulation._

import scala.util.Random
import scala.math.pow

import org.scalatest.{FunSpec, Matchers}

class VoronoiDiagramSpec extends FunSpec with Matchers {

  def rasterizePoly(poly: Polygon, tile: MutableArrayTile, re: RasterExtent, erring: Boolean)(implicit trans: Int => Point) = {
    if (erring) {
      Rasterizer.foreachCellByPolygon(poly, re){ (c,r) => tile.set(c, r, 2) }
    } else {
      val l = poly.boundary.toGeometry.get
      Rasterizer.foreachCellByGeometry(l, re){ (c,r) => tile.set(c, r, 1) }
    }
  }

  def rasterizeVoronoi(voronoi: VoronoiDiagram)(implicit trans: Int => Point): Unit = {
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
      val pts = Array((0.0,-2.0), (0.0,0.0), (0.0,1.0), (-0.5,2.0), (0.5,2.0)).map{ case (x ,y) => new Coordinate(x, y) }
      implicit val trans = { i: Int => Point.jtsCoord2Point(pts(i)) }
      val voronoi = VoronoiDiagram(pts, extent)

      def validCoveredPolygon(poly: Polygon) = {
        poly.isValid && extent.covers(poly)
      }

      voronoi.voronoiCells.forall (validCoveredPolygon(_)) should be (true)
      //rasterizeVoronoi(voronoi)
    }

    it("should work for linear input set") {
      val extent = Extent(-2.25, -3, 1, 3)
      val pts = Array((0.0,-2.0), (0.0,-1.0), (0.0,0.0), (0.0,1.0), (0.0,2.0)).map{ case (x ,y) => new Coordinate(x, y) }
      implicit val trans = { i: Int => Point.jtsCoord2Point(pts(i)) }
      val voronoi = VoronoiDiagram(pts, extent)

      def validCoveredPolygon(poly: Polygon) = {
        // println(s"Polygon cell: $poly")
        poly.isValid && extent.covers(poly)
      }

      voronoi.voronoiCells.forall (validCoveredPolygon(_)) should be (true)
      // rasterizeVoronoi(voronoi)
    }

    it("should work when some cells don't intersect the extent") {
      val extent = Extent(-2.25, 0, 1, 6)
      val pts = Array((0.0,-2.0), (0.0,-1.0), (0.0,0.0), (0.0,1.0), (0.0,2.0)).map{ case (x ,y) => new Coordinate(x, y) }
      implicit val trans = { i: Int => Point.jtsCoord2Point(pts(i)) }
      val voronoi = VoronoiDiagram(pts, extent)

      def validCoveredPolygon(poly: Polygon) = {
        // println(s"Polygon cell: $poly")
        poly.isValid && extent.covers(poly)
      }

      val cells = voronoi.voronoiCells
      (cells.forall (validCoveredPolygon(_)) && cells.length == 3) should be (true)
      // rasterizeVoronoi(voronoi)
    }

    it("should work when extent is entirely contained by a cell") {
      val extent = Extent(-2.25, 3, 1, 9)
      val pts = Array((0.0,-2.0), (0.0,-1.0), (0.0,0.0), (0.0,1.0), (0.0,2.0)).map{ case (x ,y) => new Coordinate(x, y) }
      //implicit val trans = { i: Int => Point.jtsCoord2Point(pts(i)) }
      val voronoi = VoronoiDiagram(pts, extent)

      def sameAsExtent(poly: Polygon) = {
        extent.covers(poly) && poly.covers(extent)
      }

      val cells = voronoi.voronoiCells
      (cells.length == 1 && sameAsExtent(cells(0))) should be (true)
      // rasterizeVoronoi(voronoi)
    }

    it("should accept single point inputs") {
      val extent = Extent(0, 0, 1, 1)
      val pts = Array(new Coordinate(0.25, 0.25))
      val voronoi = VoronoiDiagram(pts, extent)

      def sameAsExtent(poly: Polygon) = {
        extent.covers(poly) && poly.covers(extent)
      }

      val cells = voronoi.voronoiCells
      (cells.length == 1 && sameAsExtent(cells(0))) should be (true)      
    }

    it("should produce a Voronoi diagram from a real dataset") {
      val parksStream = getClass.getResourceAsStream("/wkt/parks_pts.wkt")
      val parksWKT = scala.io.Source.fromInputStream(parksStream).getLines.mkString
      val pts = geotrellis.vector.io.wkt.WKT.read(parksWKT).asInstanceOf[MultiPoint].points

      val dt = DelaunayTriangulation(pts.map{_.jtsGeom.getCoordinate}.toArray)

      val zoom = 12
      def crToExtent(col: Int, row: Int): Extent = {
        val xinc = 360.0 / math.pow(2, zoom)
        val yinc = 180.0 / math.pow(2, zoom)
        val x = xinc * col - 180.0
        val y = 90.0 - yinc * row
        Extent(x, y - yinc, x + xinc, y)
      }

      // val extents = for (c <- 655 to 660 ; r <- 1182 to 1193) yield crToExtent(c, r)
      val extents = for (c <- 656 to 658 ; r <- 1185 to 1187) yield crToExtent(c, r)

      val polys: Seq[Polygon] = extents.flatMap{ ex =>
        val vd = new VoronoiDiagram(dt, ex)
        vd.voronoiCells
      }

      polys.forall(_.isValid) should be (true)
    }
  }
}
