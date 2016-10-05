package geotrellis.raster.io.geotiff

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.testkit._

import org.scalatest._
import spire.syntax.cfor._

class IteratorCroppedGeoTiffSpec extends FunSpec 
  with Matchers
  with RasterMatchers
  with GeoTiffTestUtils {

  describe("Returning an array of singlebands") {
    val testGeoTiff = geoTiffPath("uncompressed/striped/bit.tif")
    val extent = Extent(0, -10, 36.9512, -6.325)

    val singlebands = SinglebandGeoTiff.streaming(testGeoTiff).cropIterator(extent, true)
    val singlebandsArray = singlebands.toArray

    val unfilled =
      SinglebandGeoTiff.streaming(testGeoTiff).cropIterator(extent, false)
    val unfilledArray = unfilled.toArray
    
    val tiffTags = TiffTagsReader.read(testGeoTiff)
    val e = tiffTags.extent
    val unroundedCol = e.width.toFloat / extent.width
    val unroundedRow = e.height.toFloat / extent.height
    val colCount = math.ceil(unroundedCol - (unroundedCol % 0.01)).toInt
    val rowCount = math.ceil(unroundedRow - (unroundedRow % 0.01)).toInt

    it("should return the correct array size") {
      val size = colCount * rowCount
      
      assert(singlebandsArray.size == size)
    }

    it("should read in the correct area for the first value") {
      val actual = SinglebandGeoTiff(testGeoTiff, extent)
      assertEqual(actual, singlebandsArray(0)._2)
    }
    
    it("should read in the correct area for the last subtile") {
      val expected = singlebandsArray(singlebandsArray.size - 1)

      val colMin =
        if (extent.xmin >= 0)
          (colCount - 1) * extent.width
        else
          ((colCount - 1) * extent.width) + extent.xmin
      
      val rowMin =
        if (extent.ymin >= 0)
          (rowCount - 1) * extent.height
        else
          ((rowCount - 1) * extent.height) + extent.ymin

      val colMax = math.min(colMin + extent.width, e.xmax)
      val rowMax = math.min(rowMin + extent.height, e.ymax)

      val lastExtent = Extent(colMin, rowMin, colMax, rowMax)
      val actual = SinglebandGeoTiff.streaming(testGeoTiff).crop(lastExtent)

      assertEqual(expected._2, actual)
    }

    it("should read in only the areas that the extent fills") {
      val cCount = e.width / extent.width
      val rCount = e.height / extent.height
      val size = cCount.toInt * rCount.toInt
      
      assert(unfilledArray.size == size)
    }

    it("should have equal areas for every SinglebandGeoTiff") {
      val filtered =
        unfilledArray.filter(x => x._1.width != extent.width && x._1.height != extent.height)

      assert(filtered.length == 0)
    }
  }
}
