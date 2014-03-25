package geotrellis.data

import geotrellis._
import geotrellis.source._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.statistics.FastMapHistogram
import geotrellis.statistics.op._

import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import org.geotools.gce.geotiff.GeoTiffFormat
import org.geotools.factory.Hints
import org.geotools.referencing.CRS
import org.geotools.coverage.grid.GridCoordinates2D



import java.io.{File,FileWriter}
import javax.imageio.ImageIO

import scala.math.{abs, round}

import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder
import org.geotools.coverage.grid.io.imageio.IIOMetadataDumper
import java.awt.image.BufferedImage

import java.awt.image.DataBuffer
import java.awt.Transparency

class GeoTiffSpec extends FunSpec with TestServer with ShouldMatchers {
  describe("A GeoTiffReader") {
    it ("should fail on non-existent files") {
      val path = "/does/not/exist.tif"
      evaluating { GeoTiff.readRaster(path) } should produce [Exception];
    }

    it ("should load correct extent & gridToMap should work") {
      val path = "src/test/resources/econic.tif"
      val raster1 = GeoTiff.readRaster(path)
      val (xmap, ymap) = raster1.rasterExtent.gridToMap(0,0)
      xmap should be (-15381.615 plusOrMinus 0.001)
      ymap should be (15418.729 plusOrMinus 0.001)
    }

    it("should correctly translate NODATA values for an int raster which has no NODATA value associated") {
      val path = "geotools/data/cea.tif"
      val raster = GeoTiff.readRaster(path)
      val (cols, rows) = (raster.cols, raster.rows)

      // Find NODATA value
      val reader = GeoTiff.getReader(path)

      val nodata = reader.getMetadata().getNoData()
      val geoRaster = reader.read(null).getRenderedImage.getData
      val data = Array.fill(cols * rows)(nodata)
      geoRaster.getPixels(0, 0, cols, rows, data)

      val rdata = raster.toArray

      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          if(isNoData(rdata(row*cols + col))) {
            val v = data(row*cols + col)
            if(isNoData(nodata))
              isNoData(v) should be (true)
            else
             v should be (nodata)
          }
        }
      }
    }

    it ("should produce same raster for GeoTiff.readRaster and through the Layer") {
      val e = Extent(-15471.6, -15511.3, 15428.4, 15388.7)
      val rasterExtent = RasterExtent(e, 60.0, 60.0, 513, 513)

      val path = "src/test/resources/econic.tif"
      val raster1 = GeoTiff.readRaster(path)//.warp(rasterExtent)

      val raster2 = GeoTiffRasterLayerBuilder.fromTif(path).getRaster()//(Some(rasterExtent))
      assertEqual(raster1,raster2)
    }

    it ("should write") {
      val r = GeoTiff.readRaster("src/test/resources/econic.tif")
      GeoTiffWriter.write("/tmp/written.tif", r, "foo")
    }

    it ("should write floating point rasters") {
      val e = Extent(100.0, 400.0, 120.0, 420.0)
      val re = RasterExtent(e, 10.0, 10.0, 2, 2)
      val data = DoubleArrayRasterData(Array(11.0, 22.0, 33.0, 44.0), 2, 2)
      val r = Raster(data, re)
      GeoTiffWriter.write("/tmp/float.tif", r, "float")
    }

    it ("should retain Float32 type when converting tif to arg") {
      val path = "src/test/resources/aspect.tif"
      val raster = GeoTiff.readRaster(path)
      raster.rasterType should be (TypeFloat)
    }

    it ("should translate NODATA correctly") {
      // This test was set up by taking an ARG, slope.arg, and converting it to TIF using GDAL.
      // Then gdalwarp was used to change the NoData value for slope.tif to -9999.
      // If the NoData values are translated correctly, then all NoData values from the read in GTiff
      // should correspond to NoData values of the directly read arg.
      import geotiff._
      val originalArg = RasterSource.fromPath("src/test/resources/data/slope.arg").get
      val translatedTif = GeoTiff.readRaster("src/test/resources/slope.tif")

      translatedTif.rows should be (originalArg.rows)
      translatedTif.cols should be (originalArg.cols)
      for(col <- 0 until originalArg.cols) {
        for(row <- 0 until originalArg.rows) {
          if(isNoData(originalArg.getDouble(col,row)))
             isNoData(translatedTif.getDouble(col,row)) should be (true)
          else
            translatedTif.getDouble(col,row) should be (originalArg.getDouble(col,row))
        }
      }
    }
  }
}
