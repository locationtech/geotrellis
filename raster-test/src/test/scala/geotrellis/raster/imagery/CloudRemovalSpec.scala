package geotrellis.raster.imagery

import geotrellis.raster.{ArrayMultibandTile, MultibandTile}
import geotrellis.raster.io.geotiff.{GeoTiffTestUtils, SinglebandGeoTiff}
import geotrellis.raster.testkit.RasterMatchers
import org.scalatest.FunSpec
import spire.syntax.cfor._

class CloudRemovalSpec extends FunSpec
    with RasterMatchers
    with GeoTiffTestUtils {

  describe("Checking cloud removal") {
    it("Pixel value should be less than original cloudy image") {

      val numImages = 3
      val multibands = Array.ofDim[MultibandTile](numImages)

      cfor(0)(_ < numImages, _ + 1) { i =>
        val red = SinglebandGeoTiff(geoTiffPath("cloud_images/red/" + (i+1) + ".TIF")).tile
        val green = SinglebandGeoTiff(geoTiffPath("cloud_images/green/" + (i+1) + ".TIF")).tile
        val blue = SinglebandGeoTiff(geoTiffPath("cloud_images/blue/" + (i+1) + ".TIF")).tile

        multibands(i) = ArrayMultibandTile(Array(red, green, blue))
      }

      // A cloudy pixel
      //print(multibands(1).band(0).get(400, 100), multibands(1).band(1).get(400, 100), multibands(1).band(2).get(400, 100))

      val cloudless = CloudRemoval.cloudRemovalMultiband(multibands)

      // Pixel value after cloud-removal
      assert(cloudless.band(0).get(400, 100) <= multibands(1).band(0).get(400, 100) &&
            cloudless.band(1).get(400, 100) <= multibands(1).band(1).get(400, 100) &&
            cloudless.band(2).get(400, 100) <= multibands(1).band(2).get(400, 100))

    }

    it("Pixel value should be less than threshold") {

      val numImages = 3
      val multibands = Array.ofDim[MultibandTile](numImages)

      cfor(0)(_ < numImages, _ + 1) { i =>
        val red = SinglebandGeoTiff(geoTiffPath("cloud_images/red/" + (i+1) + ".TIF")).tile
        val green = SinglebandGeoTiff(geoTiffPath("cloud_images/green/" + (i+1) + ".TIF")).tile
        val blue = SinglebandGeoTiff(geoTiffPath("cloud_images/blue/" + (i+1) + ".TIF")).tile

        multibands(i) = ArrayMultibandTile(Array(red, green, blue))
      }

      val threshold = 15000
      val cloudless = CloudRemoval.cloudRemovalMultiband(multibands, threshold)

      // Pixel value after cloud-removal
      assert(cloudless.band(0).get(400, 100) <= threshold &&
        cloudless.band(1).get(400, 100) <= threshold &&
        cloudless.band(2).get(400, 100) <= threshold)
    }

    it("Overloaded functions should give the same result for a specific threshold") {

      val numImages = 3
      val multibands = Array.ofDim[MultibandTile](numImages)

      cfor(0)(_ < numImages, _ + 1) { i =>
        val red = SinglebandGeoTiff(geoTiffPath("cloud_images/red/" + (i+1) + ".TIF")).tile
        val green = SinglebandGeoTiff(geoTiffPath("cloud_images/green/" + (i+1) + ".TIF")).tile
        val blue = SinglebandGeoTiff(geoTiffPath("cloud_images/blue/" + (i+1) + ".TIF")).tile

        multibands(i) = ArrayMultibandTile(Array(red, green, blue))
      }

      val threshold = 10000
      val cloudless1 = CloudRemoval.cloudRemovalMultiband(multibands)
      val cloudless2 = CloudRemoval.cloudRemovalMultiband(multibands, threshold)

      // Pixel value after cloud-removal
      assert(cloudless1.band(0).get(400, 100) == cloudless2.band(0).get(400, 100) &&
        cloudless1.band(1).get(400, 100) == cloudless2.band(1).get(400, 100) &&
        cloudless1.band(2).get(400, 100) == cloudless2.band(2).get(400, 100))
    }
  }
}
