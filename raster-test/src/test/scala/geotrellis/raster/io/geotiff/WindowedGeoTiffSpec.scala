package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.histogram._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.testkit._

import org.scalatest._
import scala.collection.mutable._
import scala.collection.JavaConversions._
import monocle.syntax.apply._
import spire.syntax.cfor._
import java.nio.file.{Files, Path, Paths}
import sys.process._

class WindowedGeoTiffSpec extends FunSpec 
  with Matchers
  with BeforeAndAfterAll
  with RasterMatchers
  with GeoTiffTestUtils
  with TileBuilders {

    override def afterAll = purge

    /*
    describe("Reading In a WindowedGeoTiff") {
      it("should be the same size as the intersecting segments") {
        val extent = Extent(0,-5,20,-4)
        val wGeoTiff = SinglebandGeoTiff.windowed(geoTiffPath("uncompressed/striped/byte.tif"), extent)
        val actual = wGeoTiff.toBytes.size

        val tiffTags = TiffTagsReader.read(geoTiffPath("uncompressed/striped/byte.tif"))
        val storage = wGeoTiff.imageData.segmentLayout.storageMethod
        val windowed = WindowedGeoTiff(extent, storage, tiffTags)
        val intersectingSegments = windowed.intersectingSegments

        val byteArrayTile = {
          val arr = Array.ofDim[Byte](windowed.cols * windowed.rows)
          var i = 0
          for (segmentId <- intersectingSegments) {
            val segment = wGeoTiff.imageData.segmentBytes.getSegment(segmentId)
            val size = segment.size
            System.arraycopy(segment, 0, arr, i, size)
          }
          UByteArrayTile.fromBytes(arr, windowed.cols, windowed.rows)
        }

        val expected = byteArrayTile.toBytes.size

        actual shouldEqual expected
      }
    }
    */
    describe("singleband GeoTiffs") {
      /*
      describe("reading striped geotiffs around the edges") {
        val extent = Extent(0, -5, 50, -4)

        it("byte") {
          val path = geoTiffPath("uncompressed/striped/byte.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
        }

        it("int16") {
          val path = geoTiffPath("uncompressed/striped/int16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
        }

        it("int32") {
          val path = geoTiffPath("uncompressed/striped/int32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
        }

        it("uint16") {
          val path = geoTiffPath("uncompressed/striped/uint16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
        }

        it("uint32") {
          val path = geoTiffPath("uncompressed/striped/uint32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
        }

        it("float32") {
          val path = geoTiffPath("uncompressed/striped/float32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
        }

        it("float64") {
          val path = geoTiffPath("uncompressed/striped/float32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
        }
      }
      */

      describe("reading striped geoTiffs in the middle") {
        val extent = Extent(10, -7, 25, -5)
        val ex = Extent(0, -7, 50, -5)
        val gridBounds = GridBounds(0, 96, 499, 303)
        val gridBounds2 = GridBounds(0, 100, 499, 299)

        it("byte") {
          val path = geoTiffPath("uncompressed/striped/byte.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(gridBounds)

          assertEqual(actual, windowedGeoTiff)
        }

        it("int16") {
          val path = geoTiffPath("uncompressed/striped/int16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(gridBounds)

          assertEqual(actual, windowedGeoTiff)
        }

        it("int32") {
          val path = geoTiffPath("uncompressed/striped/int32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(gridBounds2)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("uint16") {
          val path = geoTiffPath("uncompressed/striped/uint16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(gridBounds)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("uint32") {
          val path = geoTiffPath("uncompressed/striped/uint32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, ex)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(Extent(0, -7, 50, -5))

          assertEqual(actual, windowedGeoTiff)
        }

        it("float32") {
          val path = geoTiffPath("uncompressed/striped/float32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(gridBounds2)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("float64") {
          val path = geoTiffPath("uncompressed/striped/float64.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(gridBounds2)
          
          assertEqual(actual, windowedGeoTiff)
        }
      }

      /*
      describe("reading tiled geoTiffs around the edges") {
        val extent = Extent(0, -7, 10, -4)

        it("byte") {
          val path = geoTiffPath("uncompressed/tiled/byte.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual,windowedGeoTiff)
        }

        it("int16") {
          val path = geoTiffPath("uncompressed/tiled/int16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)
          
          assertEqual(actual,windowedGeoTiff)
        }

        it("int32") {
          val path = geoTiffPath("uncompressed/tiled/int32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)
          
          assertEqual(actual,windowedGeoTiff)
        }

        it("uint16") {
          val path = geoTiffPath("uncompressed/tiled/uint16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)
          
          assertEqual(actual,windowedGeoTiff)
        }

        it("uint32") {
          val path = geoTiffPath("uncompressed/tiled/uint32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)
          
          assertEqual(actual,windowedGeoTiff)
        }

        it("float32") {
          val path = geoTiffPath("uncompressed/tiled/float32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)
          
          assertEqual(actual,windowedGeoTiff)
        }

        it("float64") {
          val path = geoTiffPath("uncompressed/tiled/float64.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)
          
          assertEqual(actual,windowedGeoTiff)
        }
      }

      describe("reading tiled geoTiffs in the middle") {
        val extent = Extent(0, -7, 10, -4)
        val expandedExtent = Extent(0, -9.115, 25.55, -4)

        it("byte") {
          val path = geoTiffPath("uncompressed/tiled/byte.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
        }

        it("int16") {
          val path = geoTiffPath("uncompressed/tiled/int16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(expandedExtent)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("int32") {
          val path = geoTiffPath("uncompressed/tiled/int32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(expandedExtent)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("uint16") {
          val path = geoTiffPath("uncompressed/tiled/uint16.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(expandedExtent)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("uint32") {
          val path = geoTiffPath("uncompressed/tiled/uint32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(expandedExtent)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("float32") {
          val path = geoTiffPath("uncompressed/tiled/float32.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(expandedExtent)
          
          assertEqual(actual, windowedGeoTiff)
        }

        it("float64") {
          val path = geoTiffPath("uncompressed/tiled/float64.tif")
          val windowedGeoTiff = SinglebandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = SinglebandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff)
          val actual = cropper.crop(expandedExtent)
          
          assertEqual(actual, windowedGeoTiff)
        }
      }
      */
    }

    /*
   describe("multiband Geotiffs") {
     describe("reading striped geotiffs around the edges") {
       val extent = Extent(0, 0, 97.79979550080868, 88.82631518411723)

       it("byte") {
          val path = geoTiffPath("3bands/byte/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("int16") {
          val path = geoTiffPath("3bands/int16/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("int32") {
          val path = geoTiffPath("3bands/int32/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("uint16") {
          val path = geoTiffPath("3bands/uint16/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("uint32") {
          val path = geoTiffPath("3bands/uint32/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("float32") {
          val path = geoTiffPath("3bands/float32/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("float64") {
          val path = geoTiffPath("3bands/float64/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }
     }

     describe("reading striped geotiffs in the middle") {
       val extent = Extent(10, 1, 15, 15)
       val expandedExtent = Extent(0, 1, 97.799, 120)

       it("byte") {
          val path = geoTiffPath("3bands/byte/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
       }

       it("int16") {
          val path = geoTiffPath("3bands/int16/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
       }

       it("int32") {
          val path = geoTiffPath("3bands/int32/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
       }

       it("uint16") {
          val path = geoTiffPath("3bands/uint16/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
       }

       it("uint32") {
          val path = geoTiffPath("3bands/uint32/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
       }

       it("float32") {
          val path = geoTiffPath("3bands/float32/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
       }

       it("float64") {
          val path = geoTiffPath("3bands/float64/3bands-striped-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual = cropper.crop(expandedExtent)

          assertEqual(actual, windowedGeoTiff)
       }
     }

     describe("reading tiled geotiffs around the edges") {
       //val extent = Extent(0, 1, 80.684, 23.51)
       val extent = Extent(0, 5, 10, 10)

       it("byte") {
          val path = geoTiffPath("3bands/byte/3bands-tiled-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)
          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }
       it("int16") {
          val path = geoTiffPath("3bands/int16/3bands-tiled-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("int32") {
          val path = geoTiffPath("3bands/int32/3bands-tiled-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("uint16") {
          val path = geoTiffPath("3bands/uint16/3bands-tiled-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("uint32") {
          val path = geoTiffPath("3bands/uint32/3bands-tiled-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("float32") {
          val path = geoTiffPath("3bands/float32/3bands-tiled-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }

       it("float64") {
          val path = geoTiffPath("3bands/float64/3bands-tiled-band.tif")
          val windowedGeoTiff = MultibandGeoTiff.windowed(path, extent)

          val wholeGeoTiff = MultibandGeoTiff(path)
          val cropper = new RasterCropMethods(wholeGeoTiff.raster)
          val actual =
            cropper.crop(windowedGeoTiff.imageData.segmentLayout.totalCols,
              windowedGeoTiff.imageData.segmentLayout.totalRows)

          assertEqual(actual, windowedGeoTiff)
       }
       */
  }
