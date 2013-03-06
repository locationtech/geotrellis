package geotrellis.io

import geotrellis._
import geotrellis.raster.op.extent.GetRasterExtent

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class LoadFileSpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer {
  describe("LoadFile") {
    it("loads a test raster.") {
      val raster = run(LoadFile("src/test/resources/fake.img8.arg"))

      raster.get(0, 0) should be (49)
      raster.get(3, 3) should be (4)
    }

    it("should load fake.img8 with resampling") {
      val extent = run(io.LoadRasterExtentFromFile("src/test/resources/fake.img8.arg")).extent

      val resampleRasterExtent = GetRasterExtent( extent.xmin, 
                                                   extent.ymin, 
                                                   extent.xmax, 
                                                   extent.ymax, 2, 2) 
      val raster = run(io.LoadFile("src/test/resources/fake.img8.arg", resampleRasterExtent))
      raster.get(0, 0) should be (34)
      raster.get(1, 0) should be (36)
      raster.get(0, 1) should be (2)
      raster.get(1, 1) should be (4)
    }
  }
}
