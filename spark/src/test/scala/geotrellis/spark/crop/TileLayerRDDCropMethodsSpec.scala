package geotrellis.spark.crop

import geotrellis.raster._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.spark._
import geotrellis.vector.Extent

import org.scalatest.FunSpec


class TileLayerRDDCropMethodsSpec extends FunSpec with TestEnvironment {

  describe("TileLayerRDD Crop Methods") {
    val path = "raster-test/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)
    val md = rdd.metadata
    val overall = md.extent
    val Extent(xmin, ymin, xmax, ymax) = overall
    val half = Extent(xmin, ymin, xmin + (xmax - xmin) / 2, ymin + (ymax - ymin) / 2)
    val small = Extent(xmin, ymin, xmin + (xmax - xmin) / 5, ymin + (ymax - ymin) / 5)

    it("should correctly crop by the rdd extent") {
      val count = rdd.crop(overall).count
      count should be (25)
    }

    it("should correctly crop by an extent half the area of the rdd extent") {
      val cropped = rdd.crop(half)
      val count = cropped.count
      count should be (9)

      val gb = cropped.metadata.bounds.get.toGridBounds
      gb.width * gb.height should be (9)
    }

    it("should correctly crop by a small extent") {
      val cropped = rdd.crop(small)
      val count = cropped.count
      count should be (1)

      val gb = cropped.metadata.bounds.get.toGridBounds
      gb.width * gb.height should be (1)
    }
  }
}
