package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.io._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

import com.vividsolutions.jts.{ geom => jts }

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class IDWInterpolateSpec extends FunSpec 
                            with ShouldMatchers 
                            with TestServer 
                            with RasterBuilders {
  describe("IDWInterpolate") {
    it("matches a QGIS generated IDW raster") {
      val r = run(io.LoadRaster("schoolidw"))
      val re = r.rasterExtent

      val path = "src/test/resources/schoolgeo.json"

      val f = scala.io.Source.fromFile(path)
      val geoJson = f.mkString
      f.close

      val geoms = run(LoadGeoJson(geoJson))
      val points = 
        (for(g <- geoms) yield {
          Point(g.geom.asInstanceOf[jts.Point],g.data.get.get("data").getTextValue.toInt)
        }).toSeq

      val result = run(IDWInterpolate(points,re))
      var count = 0
      for(col <- 0 until re.cols) {
        for(row <- 0 until re.rows) {
          val v1 = r.get(col,row)
          val v2 = result.get(col,row)
          // Allow a small variance
          if(math.abs(v1-v2) > 1) {
            count += 1
          }
        }
      }
      withClue(s"Variance was greater than 1 $count cells.") {
        count should be (0)
      }
    }
  }
}
