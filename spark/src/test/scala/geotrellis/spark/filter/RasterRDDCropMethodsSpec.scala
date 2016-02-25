package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.spark.filter._
import geotrellis.raster.io.geotiff.SingleBandGeoTiff
import geotrellis.vector.Extent

import org.scalatest.FunSpec


class RasterRDDCropMethodsSpec extends FunSpec with TestEnvironment {

  describe("RasterRDD Crop Methods") {
    val path = "raster-test/data/aspect.tif"
    val gt = SingleBandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val (_, rdd) = createRasterRDD(originalRaster, 5, 5, gt.crs)
    val md = rdd.metadata
    val overall = md.extent
    val half = Extent(633750, 218375, 641250, 225125)
    val small = Extent(639000.0, 217700.0, 642000.0, 220399.0)
    val mt = md.mapTransform

    it("should correctly crop by the rdd extent") {
      val count = rdd.crop(overall).count
      count should be (25)
    }

    it("should correctly crop by an extent half the area of the rdd extent") {
      val count = rdd.crop(half).count
      count should be (9)
    }

    it("should correctly crop by a small extent") {
      val count = rdd.crop(small).count
      count should be (1)
    }
  }
}
