package geotrellis.feature

import geotrellis._
import geotrellis.feature._
import geotrellis.raster._
import geotrellis.io._

import org.scalatest.FunSpec
import org.scalatest.matchers._

import org.scalatest.junit.JUnitRunner
import geotrellis.testutil._


import org.apache.commons.math3.stat.regression.SimpleRegression

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class VariogramSpec extends FunSpec 
                            with ShouldMatchers 
                            with TestServer 
                            with RasterBuilders {
  describe("Variogram") {
    it("Simple Regression Test") {
      val regression = new SimpleRegression
      val points = Seq[Double,Double]((1.47,52.21),(1.50,53.12),(1.52,54.48),(1.55,55.84),(1.57,57.20),(1.60,58.57),(1.63,59.93),(1.65,61.29),(1.68,63.11),(1.70,64.47),(1.73,66.28),(1.75,68.10),(1.78,69.92),(1.80,74.46))

      for((x,y) <- points) { regression.addData(x,y) }
      val slope = Variogram.trunc(regression.getSlope,3)
      val intercept = Variogram.trunc(regression.getIntercept,3)

      val slopeExpected = 61.272
      val interceptExpected = -39.061
      withClue(s"Returned slope of $slope did not match expected slope of $slopeExpected.") {
        slope should be (61.272)
      }
    }

    // it("matches a QGIS generated IDW raster") {
    //   val r = run(io.LoadRaster("schoolidw"))
    //   val re = r.rasterExtent

    //   val path = "src/test/resources/schoolgeo.json"

    //   val f = scala.io.Source.fromFile(path)
    //   val geoJson = f.mkString
    //   f.close

    //   val geoms = run(LoadGeoJson(geoJson))
    //   val points = 
    //     (for(g <- geoms) yield {
    //       Point(g.geom.asInstanceOf[jts.Point],g.data.get.get("data").getTextValue.toInt)
    //     }).toSeq

    //   val result = run(IDWInterpolate(points,re))
    //   var count = 0
    //   for(col <- 0 until re.cols) {
    //     for(row <- 0 until re.rows) {
    //       val v1 = r.get(col,row)
    //       val v2 = result.get(col,row)
    //       // Allow a small variance
    //       if(math.abs(v1-v2) > 1) {
    //         count += 1
    //       }
    //     }
    //   }
    //   withClue(s"Variance was greater than 1 $count cells.") {
    //     count should be (0)
    //   }
    // }
  }
}
