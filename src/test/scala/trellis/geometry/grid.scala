package trellis.geometry.grid

import math.{max,min,round}

import trellis._
import trellis.raster._
import trellis.{Extent,RasterExtent}

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class GridPolygonSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("A GridPoint") {
    it("should build") {
      val p1 = new GridPoint(0, 0)
      val p2 = new GridPoint(19, 7)
    }

    it("should implement equality") {
      val pa = new GridPoint(1, 1)
      val pb = new GridPoint(1, 1)
      val pc = new GridPoint(2, 1)
      pa.equal(pb) must be === true
      pa.equal(pc) must be === false
    }

    it("should stringify") {
      new GridPoint(13, 10).toString must be === "(13, 10)"
    }
  }

  val mkline = (x1:Int, y1:Int, x2:Int, y2:Int) => {
    new GridLine(new GridPoint(x1, y1), new GridPoint(x2, y2))
  }

  describe("A GridLine") {
    it("should build") {
      val ab = new GridLine(new GridPoint(0, 0), new GridPoint(4, 12))
    }

    it("should calculate x coordinates") {
      val ab = new GridLine(new GridPoint(0, 0), new GridPoint(4, 12))
      ab.getx(0) must be === 0
      ab.getx(1) must be === 0
      ab.getx(2) must be === 1
      ab.getx(3) must be === 1
      ab.getx(6) must be === 2
      ab.getx(9) must be === 3
      ab.getx(12) must be === 4
    }

    val cd = mkline(36, 14, 0, 0)
    it("getx(0)") { cd.getx(0) must be === 0 }
    it("getx(1)") { cd.getx(1) must be === 3 }
    it("getx(7)") { cd.getx(7) must be === 18 }
    it("getx(14)") { cd.getx(14) must be === 36 }

    it("should stringify") {
      cd.toString must be === "GridLine((36, 14) -> (0, 0))"
    }
  }

  val showraster = (lines:Array[GridLine]) => {
    val p = new GridPolygon(lines)
    var width  = p.xmax + 1
    var height = p.ymax + 1

    val e = Extent(0.0, 0.0, width.toDouble, height.toDouble)
    val g = RasterExtent(e, 1.0, 1.0, width, height)

    val data = Array.fill[Int](width * height)(NODATA)
    val raster = IntRaster(data, rows=height, cols=width, rasterExtent=g)

    p.rasterize(raster, 1)
    println(raster.asciiDraw)
  }

  describe("A GridPolygon") {
    it("should fail to build when edges are wrong #1") {
      evaluating {
        showraster(Array(mkline(0, 0, 1, 2),
                         mkline(3, 4, 9, 9),
                         mkline(9, 9, 0, 0)))
      } should produce [Exception];
    }

    it("should NOT fail to build when edges are wrong #2") {
      // evaluating {
      //   showraster(Array(mkline(0, 0, -2, -2),
      //                    mkline(-2, -2, 9, 9),
      //                    mkline(9, 9, 0, 0)))
      // } should produce [Exception];
      showraster(Array(mkline(0, 0, -2, -2),
                       mkline(-2, -2, 9, 9),
                       mkline(9, 9, 0, 0)))
    }

    it("should fail to rasterize to a smaller canvas") {
      evaluating {
        val p = new GridPolygon(Array(mkline(0, 0, 2, 2),
                                      mkline(2, 2, 9, 9),
                                      mkline(9, 9, 0, 0)))
        val width = 6
        val height = 6
        val data = Array.ofDim[Int](width * height)
        p.rasterize(width, height, 1, ArrayRasterData(data), 0, 0)
      } should produce [Exception];
    }

    it("should build #1") {
      showraster(Array(mkline(0, 0, 4, 18),
                       mkline(4, 18, 36, 14),
                       mkline(36, 14, 0, 0)))
    }

    it("should build #2") {
      showraster(Array(mkline(0, 0, 0, 17),
                       mkline(0, 17, 14, 10),
                       mkline(14, 10, 0, 0)))
    }

    it("should build #3") {
      showraster(Array(mkline(0, 0, 3, 17),
                       mkline(3, 17, 19, 18),
                       mkline(19, 18, 16, 0),
                       mkline(16, 0, 0, 0)))
    }

    it("should build #4") {
      showraster(Array(mkline(2, 5, 2, 15),
                       mkline(2, 15, 8, 18),
                       mkline(8, 18, 20, 12),
                       mkline(20, 12, 6, 2),
                       mkline(6, 2, 2, 5)))
    }

    it("should build crazy #1") {
      showraster(Array(mkline(0, 0, 20, 0),
                       mkline(20, 0, 0, 18),
                       mkline(0, 18, 20, 18),
                       mkline(20, 18, 0, 0)))
    }

    it("should build crazy #2") {
      showraster(Array(mkline(0, 0, 20, 0),
                       mkline(20, 0, 0, 18),
                       mkline(0, 18, 20, 18),
                       mkline(20, 18, 0, 0)))
    }

    it("should build pointy #1") {
      showraster(Array(mkline(0, 0, 20, 0),
                       mkline(20, 0, 15, 14),
                       mkline(15, 14, 10, 2),
                       mkline(10, 2, 5, 14),
                       mkline(5, 14, 0, 0)))
    }
  }

}
