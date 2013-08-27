package geotrellis

import geotrellis.testutil._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.statistics.Histogram

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DataSourceSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  def getDistributedRasterSource = 
    DistributedRasterSource("mtsthelens_tiled_cached")

  describe("DistributedRasterSource") {
    it("should return a DistributedRasterSource when possible") { 
      val d1 = getDistributedRasterSource
      val d2:DistributedRasterSource = d1.localAdd(3)
      val d3:DistributedRasterSource  = d2.map(local.Add(_, 3))

      val result1 = runSource(d1)
      val result2 = runSource(d2)
      val result3 = runSource(d3)

      result1.get(100,100) should be (3233)
      result2.get(100,100) should be (3236)
      result3.get(100,100) should be (3239)
    }
    it ("should return a DistributedSeqSource when appropriate") {
      val d = getDistributedRasterSource
      val s:DistributedSeqSource[Histogram] = d.histogram
      println(s)
    }
  }

  describe("LocalRasterSource") {
    it("should return a LocalRasterSource when possible") {
      val l1 = LocalRasterSource.fromRaster(createOnesRaster(5))
      val l2:LocalRasterSource = l1.localAdd(2)
      val l3:LocalRasterSource = l2.localAdd(5)
      val l4:LocalRasterSource = l3.localSubtract(10)

      val result4 = runSource(l1)
      val result5 = runSource(l2)
      val result6 = runSource(l3)
      val result7 = runSource(l4)

      result4.get(0,0) should be (1)      
      result5.get(0,0) should be (3)
      result6.get(0,0) should be (8)
      result7.get(0,0) should be (-2)
    }

    
  }

 
}
