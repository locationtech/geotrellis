package geotrellis.spark.op.elevation

import geotrellis.spark._
import geotrellis.spark.io.hadoop._

import geotrellis.raster.op.focal._
import geotrellis.raster._

import org.scalatest.FunSpec

import org.apache.hadoop.fs.Path

class SlopeSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Slope Elevation Spec") {

    ifCanRunSpark {

      it("should square max for raster rdd") {
        println(inputHome)
        val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      }

    }
  }
}
