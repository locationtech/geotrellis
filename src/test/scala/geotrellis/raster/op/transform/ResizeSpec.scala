package geotrellis.raster.op.transform

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ResizeSpec extends FunSpec 
                            with ShouldMatchers 
                            with TestServer {
  describe("ResizeRaster") {
    it("should resize quad8 correctly") {

      // double number of rows and cols
      val re = RasterExtent(Extent(-9.5,3.8,150.5,163.8),4.0,4.0,40,40)
      val r = io.LoadFile("src/test/resources/quad8.arg")
      val resize1 = run(Resize(io.LoadFile("src/test/resources/quad8.arg"), re))
      
      val resize2 = run(Resize(io.LoadFile("src/test/resources/quad8.arg"), 40, 40))
      
      List(resize1, resize2).foreach { r =>
        r.cols should be (40)
        r.rows should be (40)

      	r.get(0,0) should be (1)
      	r.get(21,0) should be (2)
      	r.get(0,21) should be (3)
      	r.get(21,21) should be (4)
      }
    }
  }
    it("should resize quad8 to 4x4 correctly") {
      val raster = run(Resize(io.LoadFile("src/test/resources/quad8.arg"), 4, 4))

      raster.cols should be (4)
      raster.rows should be (4)

      val d = raster.data.asArray

      d(0) should be (1)
      d(3) should be (2)
      d(8) should be (3)
      d(11) should be (4)
    }
  

}
