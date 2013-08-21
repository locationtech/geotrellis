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
      //val d1 = DistributedRasterSource("mtsthelens_tiled_cached")
      //val d2:DistributedRasterSource = d1.localAdd(3)
      //val d3:DistributedRasterSource  = d1.map(local.Add(_, 3))
      //val d4:LocalRasterSource = d1.converge
      //val result1 = runSource(d1)
      //val result2 = runSource(d2)
      //val result3 = runSource(d3)
      //println(s"d1 is: ${result1}")
      //println(s"d2 is: ${result2}")
      //println(s"d3 is: ${result3}")

      //println(s"d1: ${ result1.get(100,100) }" )
      //println(s"d2: ${ result2.get(100,100) }" )
      //println(s"d3: ${ result3.get(100,100) }" )

      val l1 = LocalRasterSource.fromRaster(createOnesRaster(5))
      //val l2:LocalRasterSource = l1.map(local.Add(_, 3))
      val l2:LocalRasterSource = l1.localAdd(2)
      //val l3:LocalRasterSource = l1.localAdd(2)

      //val result4 = runSource(l1)
      val result5 = runSource(l2)
      //val result6 = runSource(l3)

      //println(s"l1 is: ${ result4 }")
      println(s"l2 is: ${ result5 }")
      //println(s"l3 is: ${ result6 }")

      //println(s"l1: ${ result4.get(1,1) }" )
      println(s"l2: ${ result5.get(2,2) }" )
      //println(s"l3: ${ result6.get(3,3) }" )


    }
  }
}
