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
      val d1 = DistributedRasterSource("mtsthelens_tiled_cached")
      val d2:DistributedRasterSource = d1.localAdd(3)
      val d3:DistributedRasterSource  = d1.map(local.Add(_, 3))

     // val l1 = new LocalRasterSource(null)
    //  val l2:LocalRasterSource = l1.map(local.Add(_, 3))
    //  val l3:LocalRasterSource = l1.localAdd(2)
     }
  }
}
