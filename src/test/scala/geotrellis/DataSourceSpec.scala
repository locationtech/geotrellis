package geotrellis

import geotrellis.testutil._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.raster._
import geotrellis.raster.op._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DataSourceSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  describe("DistributedRasterData") {
    it("should return a DistributedRasterData from map when possible") { 
      val r = byteRaster
      val d1 = new DistributedRasterSource(null)
      val d2 = d1.localAdd(3)

      d2 should be (DistributedRasterSource)

      val d3  = d1.map(local.Add(_, 3))

      val l1 = new LocalRasterSource(null)
      val l2 = l1.map(local.Add(_, 3))
      l2 should be (LocalRasterSource)
      
      val l3 = l1.localAdd(2)
      l3 should be (LocalRasterSource)
    }
  }
}
