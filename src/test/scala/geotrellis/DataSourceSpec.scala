package geotrellis

import geotrellis.testutil._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.statistics._
import geotrellis.source.{RasterSource,DataSource}

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
      val d3:RasterSource  = d2 mapOp(local.Add(_, 3))
      val d4:RasterSource = d3 map(r => r.map(z => z + 3))
      val d5:DataSource[Int,Seq[Int]] = d3 map(r => r.findMinMax._2)
      
      val e2 = d1.localAdd(3)
      val e3 = d2 mapOp(local.Add(_, 3))
      val e4 = d3 map(r => r.map(z => z + 3))
      val e5 = d3 map(r => r.findMinMax._2)

      val result1 = runSource(d1)
      val result2 = runSource(d2)
      val result3 = runSource(d3)
      val result4 = runSource(d4)
      val result5 = runSource(d5)

      result1.get(100,100) should be (3233)
      result2.get(100,100) should be (3236)
      result3.get(100,100) should be (3239)
      result4.get(100,100) should be (3242)
      result5.head should be (6026)
    }

    it ("should handle a histogram result") {
      val d = getRasterSource

      val hist = d.histogram
      val hist2:DataSource[Histogram,Histogram] = d.map( (h:Raster) => FastMapHistogram() )
      case class MinFromHistogram(h:Op[Histogram]) extends Op1(h)({
        (h) => Result(h.getMinValue)
      })

      case class FindMin(ints:Op[Seq[Int]]) extends Op1(ints)({
        (ints) => Result(ints.reduce(math.min(_,_)))
      })

      val ints:DataSource[Int,Seq[Int]] = hist.mapOp(MinFromHistogram(_))

      val int = ints.converge.map( seqInt => seqInt.reduce(math.min(_,_)))

      val histogramResult = runSource(hist)
      val intsResult = runSource(ints)
      
      histogramResult.getMinValue should be (2231)
      histogramResult.getMaxValue should be (8367)
      intsResult.length should be (12)
  
    }

  }
}
