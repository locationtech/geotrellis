package geotrellis.data

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.statistics.FastMapHistogram
import geotrellis.statistics.op._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import org.geotools.gce.geotiff.{GeoTiffFormat}
import org.geotools.factory.{Hints}
import org.geotools.referencing.{CRS}
import org.geotools.coverage.grid.{GridCoordinates2D}

import java.io.{File,FileWriter}
import javax.imageio.ImageIO

import scala.math.{abs, round}

import org.geotools.coverage.grid.io.imageio.geotiff.{GeoTiffIIOMetadataDecoder}
import org.geotools.coverage.grid.io.imageio.IIOMetadataDumper
import java.awt.image.BufferedImage

import Console.printf
import java.awt.image.DataBuffer
import java.awt.Transparency

import org.geotools.gce.geotiff.{GeoTiffFormat}
import org.geotools.factory.{Hints}
import org.geotools.referencing.{CRS}
import org.geotools.coverage.grid.{GridCoordinates2D}

object TestServer {
  lazy val server:Server = new Server("testutil", Catalog.fromPath("src/test/resources/catalog.json"))
}

class GeoTiffSpec extends FunSpec with MustMatchers with ShouldMatchers {
  val server = TestServer.server

  describe("A GeoTiffReader") {
    it ("should fail on non-existent files") {
      val path = "/does/not/exist.tif"
      evaluating { new GeoTiffReader(path).readPath(None, None) } should produce [Exception];
    }

    it ("should load correct extent & gridToMap should work") {
      val path = "src/test/resources/econic.tif"
      val raster1 = new GeoTiffReader(path).readPath(None, None)
      val (xmap, ymap) = raster1.rasterExtent.gridToMap(0,0)
      xmap should be (-15381.615 plusOrMinus 0.001)
      ymap should be (15418.729 plusOrMinus 0.001)
    }

    it ("should render to PNG") {
      val path = "src/test/resources/econic.tif"
      val raster1 = new GeoTiffReader(path).readPath(None, None)

      val e = Extent(-15471.6, -15511.3, 15428.4, 15388.7)
      val geo = RasterExtent(e, 60.0, 60.0, 513, 513)
      val raster2 = new GeoTiffReader(path).readPath(None, Some(geo))
    }

    it ("should draw") {
      val path = "src/test/resources/econic.tif"
      val raster = new GeoTiffReader(path).readPath(None, None)
      val histogram = FastMapHistogram.fromRaster(raster)

      val (zmin, zmax) = raster.findMinMax

      val chooser = new MultiColorRangeChooser(Array(0xFF0000FF, 0xFFFF00FF, 0x0000FFFF))
      val breaks = (zmin to zmax).toArray
      val colors = chooser.getColors(breaks.length)
      val cb = ColorBreaks.assign(breaks, colors)

      server.run(io.WritePng(raster, "/tmp/fromgeo.png", cb, histogram, NODATA))
    }

    it ("should write") {
      val r = new GeoTiffReader("src/test/resources/econic.tif").readPath(None, None)
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
      val raster = new GeoTiffReader(path).readPath(None, None)
      raster.data.getType should be (TypeFloat)
    }

    it ("should translate NODATA correctly") {
      // This test was set up by taking an ARG, slope.arg, and converting it to TIF using GDAL.
      // Then gdalwarp was used to change the NoData value for slope.tif to -9999.
      // If the NoData values are translated correctly, then all NoData values from the read in GTiff
      // should correspond to NoData values of the directly read arg.
      import geotiff._
      val originalArg = server.run(io.LoadFile("src/test/resources/data/slope.arg"))
      val translatedTif = new GeoTiffReader("src/test/resources/slope.tif").readPath(None, None)

      translatedTif.rows should be (originalArg.rows)
      translatedTif.cols should be (originalArg.cols)
      for(col <- 0 until originalArg.cols) {
        for(row <- 0 until originalArg.rows) {
          if(originalArg.getDouble(col,row).isNaN)
             translatedTif.getDouble(col,row).isNaN should be (true)
          else
            translatedTif.getDouble(col,row) should be (originalArg.getDouble(col,row))
        }
      }
    }
  }
}
