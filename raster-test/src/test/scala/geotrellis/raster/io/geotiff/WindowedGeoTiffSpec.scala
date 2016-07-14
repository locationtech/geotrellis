package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.testkit._

import org.scalatest._
import scala.collection.JavaConversions._
import monocle.syntax.apply._
import spire.syntax.cfor._
import sys.process._

object Reader {
  def singleBand(path: String, extent: Extent): (SinglebandGeoTiff, SinglebandGeoTiff, WindowedGeoTiff) = {
    val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)
    val whole = SinglebandGeoTiff(path)
    val tiffTags = TiffTagsReader.read(path)
    val windowed =
      WindowedGeoTiff(extent, whole.imageData.segmentLayout.storageMethod, tiffTags)
    (windowedGeoTiff, whole, windowed)
  }
  def multiBand(path: String, extent: Extent): (MultibandGeoTiff, MultibandGeoTiff, WindowedGeoTiff) = {
    val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)
    val whole = MultibandGeoTiff(path)
    val tiffTags = TiffTagsReader.read(path)
    val windowed = WindowedGeoTiff(extent, whole.imageData.segmentLayout.storageMethod, tiffTags)
    (windowedGeoTiff, whole, windowed)
  }
}

class WindowedGeoTiffSpec extends FunSpec 
  with Matchers
  with BeforeAndAfterAll
  with RasterMatchers
  with GeoTiffTestUtils
  with TileBuilders {

  describe("windowed, singleband GeoTiffs") {
    val bitStriped = geoTiffPath("uncompressed/striped/bit.tif")
    val byteStriped = geoTiffPath("uncompressed/striped/byte.tif")
    val int16Striped = geoTiffPath("uncompressed/striped/int16.tif")
    val int32Striped = geoTiffPath("uncompressed/striped/int32.tif")
    val uint16Striped = geoTiffPath("uncompressed/striped/uint16.tif")
    val uint32Striped = geoTiffPath("uncompressed/striped/uint32.tif")
    val float32Striped = geoTiffPath("uncompressed/striped/float32.tif")
    val float64Striped = geoTiffPath("uncompressed/striped/float64.tif")
    
    val bitTiled = geoTiffPath("uncompressed/tiled/bit.tif")
    val byteTiled = geoTiffPath("uncompressed/tiled/byte.tif")
    val int16Tiled = geoTiffPath("uncompressed/tiled/int16.tif")
    val int32Tiled = geoTiffPath("uncompressed/tiled/int32.tif")
    val uint16Tiled = geoTiffPath("uncompressed/tiled/uint16.tif")
    val uint32Tiled = geoTiffPath("uncompressed/tiled/uint32.tif")
    val float32Tiled = geoTiffPath("uncompressed/tiled/float32.tif")
    val float64Tiled = geoTiffPath("uncompressed/tiled/float64.tif")

    /*
    describe("reading striped geotiffs around the edges") {
      val extent = Extent(0, -6, 50, -4)
      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual, windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
    }
    describe("reading striped geoTiffs in the middle") {
      val extent = Extent(10, -8, 25, -5)
      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual, windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
    }
    */

    describe("reading tiled geoTiffs around the edges") {
      val extent = Extent(0, -10, 12, -8)

      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual,windowedGeoTiff)
      }

      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }
    }

    describe("reading tiled geoTiffs in the middle") {
      val extent = Extent(48, -10, 48.68, -4)
      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual,windowedGeoTiff)
      }
      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }
      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.singleBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }
    }
  }

  describe("multiband Geotiffs") {
    val bitStriped = geoTiffPath("3bands/bit/3bands-striped-band.tif")
    val byteStriped = geoTiffPath("3bands/byte/3bands-striped-band.tif")
    val int16Striped = geoTiffPath("3bands/int16/3bands-striped-band.tif")
    val int32Striped = geoTiffPath("3bands/int32/3bands-striped-band.tif")
    val uint16Striped = geoTiffPath("3bands/uint16/3bands-striped-band.tif")
    val uint32Striped = geoTiffPath("3bands/uint32/3bands-striped-band.tif")
    val float32Striped = geoTiffPath("3bands/float32/3bands-striped-band.tif")
    val float64Striped = geoTiffPath("3bands/float64/3bands-striped-band.tif")
    
    val bitTiled = geoTiffPath("3bands/bit/3bands-tiled-band.tif")
    val byteTiled = geoTiffPath("3bands/byte/3bands-tiled-band.tif")
    val int16Tiled = geoTiffPath("3bands/int16/3bands-tiled-band.tif")
    val int32Tiled = geoTiffPath("3bands/int32/3bands-tiled-band.tif")
    val uint16Tiled = geoTiffPath("3bands/uint16/3bands-tiled-band.tif")
    val uint32Tiled = geoTiffPath("3bands/uint32/3bands-tiled-band.tif")
    val float32Tiled = geoTiffPath("3bands/float32/3bands-tiled-band.tif")
    val float64Tiled = geoTiffPath("3bands/float64/3bands-tiled-band.tif")

    /*
    describe("reading striped geotiffs around the edges") {
      val extent = Extent(0, 1.5, 97.79, 88.82)
      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual, windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
    }
    describe("reading striped geoTiffs in the middle") {
      val extent = Extent(10, 1.5, 15, 15)
      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual, windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }
    }
    */
    describe("reading tiled geoTiffs around the edges") {
      val extent = Extent(0, 1.5, 3, 10)
      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual,windowedGeoTiff)
      }
      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }
    }
    
    describe("reading tiled geoTiffs in the middle") {
      val extent = Extent(3, 5, 7, 15)
      it("bit") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual, windowedGeoTiff)
      }

      it("byte") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)

        assertEqual(actual,windowedGeoTiff)
      }

      it("int16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("int32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint16") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("uint32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float32") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }

      it("float64") {
        val (windowedGeoTiff, wholeGeoTiff, windowed) = Reader.multiBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(windowed.windowedGridBounds)
        
        assertEqual(actual,windowedGeoTiff)
      }
    }
  }
}
