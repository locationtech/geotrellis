package geotrellis

import geotrellis.testutil._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.statistics._
import geotrellis.source._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DataSourceSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  def getRasterSource = 
    RasterSource("mtsthelens_tiled_cached")

  describe("RasterSource") {
    it("should return a RasterSource when possible") { 
      val d1 = getRasterSource
      val d2:RasterSource = d1.localAdd(3)
      val d3:RasterSource  = d2.mapOp(local.Add(_, 3))

      val result1 = runSource(d1)
      val result2 = runSource(d2)
      val result3 = runSource(d3)

      result1.get(100,100) should be (3233)
      result2.get(100,100) should be (3236)
      result3.get(100,100) should be (3239)
    }

/*
    it ("should return a DistributedSeqSource when appropriate") {
      val d = getRasterSource
      // distributed source of histograms

      // DataSource[Histogram,_] -- we can get a single histogram
      //val hist = d.histogram // distributed
      
      case class MinFromHistogram(h:Op[Histogram]) extends Op1(h)({
        (h) => Result(h.getMinValue)
      })

      case class FindMin(ints:Op[Seq[Int]]) extends Op1(ints)({
        (ints) => Result(ints.reduce(math.min(_,_)))
      })

      val ints:SeqDataSource[Int] = hist.map(MinFromHistogram(_))

      //val int = ints.converge.map(FindMin(_))
      //println(s"Int is: $int")

      //val histogramResult = runSource(hist)
      //val intsResult = runSource(ints)
      println(s"ints result: $intsResult")
      val s = histogramResult
      println(s)
      println(histogramResult)
      histogramResult.getMinValue should be (2231)
      histogramResult.getMaxValue should be (8367)
      intsResult.length should be (12)
  //    minResult should be (2231)
    }
 */
  }
 
}
