package geotrellis.io

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class LoadFileSpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer 
                      with RasterBuilders {
  describe("LoadFile") {
    it("loads a test raster.") {
      val raster = get(LoadFile("src/test/resources/fake.img8.arg"))

      raster.get(0, 0) should be (49)
      raster.get(3, 3) should be (4)
    }

    it("should load fake.img8 with resampling") {
      val realRaster = get(io.LoadFile("src/test/resources/fake.img8.arg"))
      val re = get(io.LoadRasterExtentFromFile("src/test/resources/fake.img8.arg"))
      val extent = re.extent

      val resampleRasterExtent = RasterExtent(extent, 2, 2) 
      val raster = get(io.LoadFile("src/test/resources/fake.img8.arg", resampleRasterExtent))
      printR(realRaster)
      println(re)
      printR(raster)
      println(resampleRasterExtent)

      raster.get(0, 0) should be (34)
      raster.get(1, 0) should be (36)
      raster.get(0, 1) should be (2)
      raster.get(1, 1) should be (4)
    }
  }
}
