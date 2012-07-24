package geotrellis.op

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import geotrellis.geometry.Polygon

import geotrellis.data.ColorBreaks
import geotrellis.Raster
import geotrellis.{Extent,RasterExtent}

import geotrellis._
import geotrellis.statistics._
import geotrellis.raster.op._
import geotrellis.process._

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
      Raster(a, g)
    }
    val f = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
             cellsize:Double) => f2(a, cols, rows, xmin, ymin, cellsize, 999)
  
    val a = Array(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val r = Literal(f(a, 3, 3, 0.0, 0.0, 1.0))

    it ("DoCell") {
      val r2 = server.run(local.DoCell(r, _ + 1))
      val d = r2.data.asArray.getOrElse(sys.error("argh"))
      d.toArray must be === a.map { _ + 1 }
    }
  }
}
