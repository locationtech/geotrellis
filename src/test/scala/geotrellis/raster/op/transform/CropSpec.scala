package geotrellis.raster.op.transform

import geotrellis._

import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CropSpec extends FunSpec with ShouldMatchers 
                               with TestServer
                               with RasterBuilders {
  describe("Crop") {
    it("should crop raster to inner raster") {
      val r = createRaster(Array[Int]( 1, 1, 1, 1, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 1, 1, 1, 1))

      val innerExtent = Extent(1,1,4,4)
      assertEqual(Crop(r,innerExtent), Array[Int](2, 2, 2,
                                                  2, 2, 2,
                                                  2, 2, 2))
    }

    it("should crop one row off raster") {
      val r = createRaster(Array[Int]( 1, 1, 1, 1, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 1, 1, 1, 1))

      val innerExtent = Extent(0,1,5,5)
      assertEqual(Crop(r,innerExtent), Array[Int](1, 1, 1, 1, 1,
                                                  1, 2, 2, 2, 1,
                                                  1, 2, 2, 2, 1,
                                                  1, 2, 2, 2, 1))
      val innerExtent2 = Extent(0,0,5,4)
      assertEqual(Crop(r,innerExtent2), Array[Int](1, 2, 2, 2, 1,
                                                  1, 2, 2, 2, 1,
                                                  1, 2, 2, 2, 1,
                                                  1, 1, 1, 1, 1))

    }

    it("should crop one column off raster") {
      val r = createRaster(Array[Int]( 1, 1, 1, 1, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 1, 1, 1, 1))

      val innerExtent = Extent(1,0,5,5)
      assertEqual(Crop(r,innerExtent), Array[Int](1, 1, 1, 1,
                                                  2, 2, 2, 1,
                                                  2, 2, 2, 1,
                                                  2, 2, 2, 1,
                                                  1, 1, 1, 1))
      val innerExtent2 = Extent(0,0,4,5)
      assertEqual(Crop(r,innerExtent2), Array[Int](1, 1, 1, 1,
                                                  1, 2, 2, 2,
                                                  1, 2, 2, 2,
                                                  1, 2, 2, 2,
                                                  1, 1, 1, 1))

    }

    it("should crop raster with no data on larger crop extent than raster extent") {
      val r = createRaster(Array[Int]( 1, 1, 1, 1, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 2, 2, 2, 1,
                                       1, 1, 1, 1, 1))

      val innerExtent = Extent(1,0,6,5)
      assertEqual(Crop(r,innerExtent), Array[Int](1, 1, 1, 1, NODATA,
                                                  2, 2, 2, 1, NODATA,
                                                  2, 2, 2, 1, NODATA,
                                                  2, 2, 2, 1, NODATA,
                                                  1, 1, 1, 1, NODATA))

      val innerExtent2 = Extent(0,1,5,6)
      assertEqual(Crop(r,innerExtent2), Array[Int](NODATA,NODATA,NODATA,NODATA,NODATA,
                                                   1, 1, 1, 1, 1,
                                                   1, 2, 2, 2, 1,
                                                   1, 2, 2, 2, 1,
                                                   1, 2, 2, 2, 1))                                       
    }

  }
}
