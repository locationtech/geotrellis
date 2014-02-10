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

  describe("fromSources") {
    it("should combine sources") {
      val seq = Seq(
        ValueSource(1),
        ValueSource(2),
        ValueSource(3),
        DataSource.fromValues(1,2,1).reduce(_+_)
      )
      seq.get.toArray should be (Array(1,2,3,4))
    }

    it("should implicitly have collectSources") {
      val seq = Seq(
        ValueSource(1),
        ValueSource(2),
        ValueSource(3),
        DataSource.fromValues(1,2,1).reduce(_+_)
      )
      seq.collectSources.get.toArray should be (Array(1,2,3,4))
    }

    it("should implicitly have collectSources for ValueSource sequence") {
      val seq: Seq[ValueSource[Int]] = Seq(
        ValueSource(1),
        ValueSource(2),
        ValueSource(3)
      )
      seq.collectSources.get.toArray should be (Array(1,2,3))
    }
  }
}
