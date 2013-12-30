package geotrellis.source

import geotrellis._
import geotrellis.testutil._
import geotrellis.process._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.statistics._

class DataSourceSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  describe("DataSource") {
    it("should cache") {
      case class MockOperation(f:()=>Unit) extends Op1(f)({ f => f(); Result(true) })

      val wasRan = Array.fill[Int](4)(0)
      val mockOps = 
        (0 until 4).map { i =>
          MockOperation { () => wasRan(i) += 1 } 
        }

      val ds = DataSource(mockOps.toSeq)
      wasRan should be (Array(0,0,0,0))
      val dsCached = ds.cached
      wasRan should be (Array(1,1,1,1))
      dsCached.run match {
        case Complete(v,_) =>
          v.toArray should be (Array(true,true,true,true))
          wasRan should be (Array(1,1,1,1))
        case Error(msg,_) =>
          sys.error(msg)
      }
    }
  }
}
