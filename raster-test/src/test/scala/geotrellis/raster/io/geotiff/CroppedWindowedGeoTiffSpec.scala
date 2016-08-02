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
  def singleBand(path: String, extent: Extent): (SinglebandGeoTiff, SinglebandGeoTiff) = {
    val croppedGeoTiff = SinglebandGeoTiff.windowed(path, extent)
    val whole = SinglebandGeoTiff(path)
    (croppedGeoTiff, whole)
  }
  def multiBand(path: String, extent: Extent): (MultibandGeoTiff, MultibandGeoTiff) = {
    val croppedGeoTiff = MultibandGeoTiff.windowed(path, extent)
    val whole = MultibandGeoTiff(path)
    (croppedGeoTiff, whole)
  }
}

class CroppedGeoTiffSpec extends FunSpec 
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

    describe("reading striped geotiffs around the edges") {
      val extent = Extent(0, -7.3, 27.55, -4)
      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual, croppedGeoTiff)
      }
      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
    }
    
    describe("reading striped geoTiffs in the middle") {
      val extent = Extent(40, -8.25, 45.86, -8)
      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual, croppedGeoTiff)
      }

      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
    }

    describe("reading tiled geoTiffs around the edges") {
      val extent = Extent(0, -7.3, 25.5, -4)
      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual,croppedGeoTiff)
      }
      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }
      
      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }
    }

    describe("reading tiled geoTiffs in the middle") {
      val extent = Extent(10, -8.25, 15, -8)
      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)

        assertEqual(actual,croppedGeoTiff)
      }
      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.singleBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
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

    describe("reading striped geotiffs around the edges") {
      val extent = Extent(0, 1.5, 97.79, 88.82)
      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual, croppedGeoTiff)
      }

      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
    }

    describe("reading striped geoTiffs in the middle") {
      val extent = Extent(7, 1.5, 15, 15)
      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(bitStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(byteStriped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual, croppedGeoTiff)
      }

      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint16Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float32Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float64Striped, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }
    }

    describe("reading tiled geoTiffs around the edges") {
      val extent = Extent(0, 1.5, 3, 8)

      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual,croppedGeoTiff)
      }

      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }
      
      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }
    }

    describe("reading tiled geoTiffs in the middle") {
      val extent = Extent(4, 5, 7, 15)
      it("bit") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(bitTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual, croppedGeoTiff)
      }

      it("byte") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(byteTiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)

        assertEqual(actual,croppedGeoTiff)
      }
      it("int16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("int32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(int32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint16") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint16Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("uint32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(uint32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("float32") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float32Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }

      it("float64") {
        val (croppedGeoTiff, wholeGeoTiff) = Reader.multiBand(float64Tiled, extent)
        val cropper = new RasterCropMethods(wholeGeoTiff.raster)
        val actual = cropper.crop(extent)
        
        assertEqual(actual,croppedGeoTiff)
      }
    }
  }
}
