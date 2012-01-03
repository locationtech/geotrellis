package trellis.operation

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import trellis.geometry.Polygon

import trellis.data.ColorBreaks
import trellis.raster.IntRaster
import trellis.{Extent,RasterExtent}

import trellis.stat._
import trellis.process._
import trellis.constant._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DoCellSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("the DoCell operation") {
    val rasterExtent = RasterExtent(Extent(0.0, 0.0, 100.0, 80.0), 20.0, 20.0, 5, 4)
    val server = TestServer()
    val nd = NODATA
  
    val data1 = Array(12, 12, 13, 14, 15,
                      44, 91, nd, 11, 95,
                      12, 13, 56, 66, 66,
                      44, 91, nd, 11, 95)

    val f2 = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
             cellsize:Double, srs:Int) => {
      val g = RasterExtent(Extent(xmin, ymin, xmin + cellsize * cols, ymin + cellsize * rows),
                               cellsize, cellsize, cols, rows)
      IntRaster(a, cols, rows, g)
    }
    val f = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
             cellsize:Double) => f2(a, cols, rows, xmin, ymin, cellsize, 999)
  
    val a = Array(1, 2, 3, 4, 5, 6, 7, 8, 9)
    //val raster1 = IntRaster(data1, 4, 5, rasterExtent)
    //val r = WrapRaster(raster1)
    val r = CopyRaster(WrapRaster(f(a, 3, 3, 0.0, 0.0, 1.0)))

    it ("DoCell") {
      server.run(DoCell(r, _ + 1)).data.asArray must be === a.map { _ + 1 }
    }
  }
}
