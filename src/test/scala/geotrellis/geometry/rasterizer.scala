package geotrellis.geometry.rasterizer

import math.{max,min,round}

import geotrellis._
import geotrellis.geometry.{Polygon}
import geotrellis.geometry.grid.{GridPoint, GridLine, GridPolygon}

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

object RasterizerSpec {
  def draw(cols:Int, rows:Int, polypts:Array[Array[(Double, Double)]],
          values:Array[Int]) {
    draw(cols, rows, polypts, values.map((x:Int) => _))
  }

  def draw(cols:Int, rows:Int, polypts:Array[Array[(Double, Double)]],
          fs:Array[Int => Int]) {
    val polygons = polypts.map(Polygon(_, 0, null))

    val e = Extent(0.0, 0.0, cols, rows)
    val g = RasterExtent(e, cellwidth=1.0, cellheight=1.0, cols=cols, rows=rows)

    val data   = Array.fill[Int](cols * rows)(NODATA)
    val raster = IntRaster(data, rasterExtent=g)

    Rasterizer.rasterize(raster, polygons, fs)
    println(raster.asciiDraw)
  }
}

class RasterizerSpec extends Spec with MustMatchers with ShouldMatchers {
  val mkline = (x1:Int, y1:Int, x2:Int, y2:Int) => {
    new GridLine(new GridPoint(x1, y1), new GridPoint(x2, y2))
  }

  val showraster = (lines:Array[GridLine]) => {
    val p = new GridPolygon(lines)
    var width  = p.xmax + 1
    var height = p.ymax + 1

    val e = Extent(0.0, 0.0, width.toDouble, height.toDouble)
    val g = RasterExtent(e, cellwidth=1.0, cellheight=1.0, cols=width, rows=height)

    val data = Array.fill[Int](width * height)(NODATA)
    val raster = IntRaster(data, rasterExtent=g)

    val polygons = Array(p)
    val values   = Array(19)
    Rasterizer.rasterize(raster, polygons, values)
    println(raster.asciiDraw)
  }

  describe("A Rasterizer") {
    it("should rasterize #1") {
      RasterizerSpec.draw(40, 20,
                          Array(Array((0.0, 0.0), (15.0, 3.0), (6.0, 17.0), (0.0, 0.0)),
                                Array((7.0, 7.0), (25.0, 19.0), (23.0, 2.0), (7.0, 7.0)),
                                Array((0.0, 18.0), (4.0, 16.0), (1.0, 13.0), (0.0, 18.0)),
                                Array((3.0, 3.0), (5.0, 5.0), (3.0, 5.0), (3.0, 3.0))),
                          Array(0x11, 0x22, 0x33, 0x44))
    }

    it("should rasterize #2") {
      RasterizerSpec.draw(40, 20,
                          Array(Array((9.0, 1.0), (16.0, 12.0), (5.0, 12.0), (9.0, 1.0)),

                                Array((1.0, 2.0), (6.0, 1.0), (5.0, 6.0), (1.0, 2.0)),

                                Array((3.0, 10.0), (7.0, 10.0), (7.0, 17.0), (3.0, 17.0), (3.0, 10.0)),

                                Array((14.0, 4.0), (19.0, 14.0), (10.0, 15.0), (14.0, 4.0)),

                                Array((1.0, 18.0), (1.0, 14.0), (8.0, 14.0), (8.0, 18.0), (1.0, 18.0))),

                          Array(0x11, 0x22, 0x33, 0x44, 0x55))
    }
  }
}
