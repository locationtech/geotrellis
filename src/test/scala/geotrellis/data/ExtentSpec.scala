package geotrellis.data

import geotrellis._
import geotrellis.operation._
import geotrellis.process._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ExtentSpec extends Spec with MustMatchers with ShouldMatchers {

  def re(pt:(Double, Double), cs:(Double, Double), ncols:Int, nrows:Int) = {
    val (x1, y1) = pt
    val (cw, ch) = cs
    val e = Extent(x1, y1, x1 + ncols * cw, y1 + nrows * ch)
    RasterExtent(e, cw, ch, ncols, nrows)
  }

  def load(name:String) = LoadRaster(name)
  def load(name:String, pt:(Double, Double), cs:(Double, Double), ncols:Int, nrows:Int) = {
    LoadRaster(name, re(pt, cs, ncols, nrows))
  }

  def xload(path:String) = LoadFile(path)
  def xload(path:String, pt:(Double, Double), cs:(Double, Double), ncols:Int, nrows:Int) = {
    LoadFile(path, re(pt, cs, ncols, nrows))
  }

  describe("An Extent") {
    val server = TestServer("src/test/resources/catalog.json")

    def confirm(op:Op[Raster], expected:Array[Int]) {
      val r = server.run(op)
      //println(r.asciiDraw)
      r.data.asArray must be === expected
    }

    val origin1 = (1100.0, 1200.0)
    val cellSizes1 = (100.0, 100.0)

    //val name = "6x6int32"
    //val path = "src/test/resources/data/6x6int32.arg"

    val name = "6x6int8"
    val path = "src/test/resources/data/6x6int8.arg"

    val expected1 = (1 to 36).toArray

    val expected23 = Array(0x08, 0x09, 0x0A,
                           0x0E, 0x0F, 0x10,
                           0x14, 0x15, 0x16)

    val expected4 = Array(0x0E, 0x0F,
                          0x14, 0x15)

    val op1 = load(name)
    val op2 = load(name, origin1, cellSizes1, 3, 3)
    val op3 = WarpRaster(op1, re(origin1, cellSizes1, 3, 3))
    val op4 = WarpRaster(op2, re(origin1, cellSizes1, 2, 2))
    confirm(op1, expected1)
    confirm(op2, expected23)
    confirm(op3, expected23)
    confirm(op4, expected4)

    val xop1 = xload(path)
    val xop2 = xload(path, origin1, cellSizes1, 3, 3)
    val xop3 = WarpRaster(xop1, re(origin1, cellSizes1, 3, 3))
    val xop4 = WarpRaster(xop2, re(origin1, cellSizes1, 2, 2))
    confirm(xop1, expected1)
    confirm(xop2, expected23)
    confirm(xop3, expected23)
    confirm(xop4, expected4)
  }
}
