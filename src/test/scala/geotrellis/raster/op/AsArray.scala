package geotrellis.raster.op

import geotrellis.testutil._
import geotrellis.raster.op.data._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AsArraySpec extends FunSpec with ShouldMatchers 
                                  with TestServer 
                                  with RasterBuilders {
  describe("AsArray") {
    it("should convert int with AsArray") {
      var arr = Array(1,2,3,4,5,6,7)
      val r = createRaster(arr)
      val arr2 = run(AsArray(r))
      arr2 should be (arr)
    }

    it("should convert double with AsArrayDouble") {
      var arr = Array(1.0,2.0,3.0,4.0,5.0,6.0,7.0)
      val r = createRaster(arr)
      val arr2 = server.run(AsArrayDouble(r))
      arr2 should be (arr)
    }
  }
}
