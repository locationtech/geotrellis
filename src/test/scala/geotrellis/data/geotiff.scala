package geotrellis.data

import geotrellis.process.TestServer
import geotrellis._
import geotrellis.raster._

import geotrellis._

import org.scalatest.Spec
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

//xyz
import Console.printf
import java.awt.image.DataBuffer
import java.awt.Transparency

import org.geotools.gce.geotiff.{GeoTiffFormat}
import org.geotools.factory.{Hints}
import org.geotools.referencing.{CRS}
import org.geotools.coverage.grid.{GridCoordinates2D}
//xyz

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GeoTiffSpec extends Spec with MustMatchers with ShouldMatchers {
  val server = TestServer()

  describe("A GeoTiffReader") {
    it ("should fail on non-existent files") {
      val path = "/does/not/exist.tif"
      evaluating { GeoTiffReader.readPath(path, None, None) } should produce [Exception];
    }

    it ("should load correct extent & gridToMap should work") {
      val path = "src/test/resources/econic.tif"
      val raster1 = GeoTiffReader.readPath(path, None, None)
      val (xmap, ymap) = raster1.rasterExtent.gridToMap(0,0)
      xmap should be (-15381.615 plusOrMinus 0.001)
      ymap should be (15418.729 plusOrMinus 0.001)
    }

    it ("should render to PNG") {
      val path = "src/test/resources/econic.tif"
      val raster1 = GeoTiffReader.readPath(path, None, None)

      val e = Extent(-15471.6, -15511.3, 15428.4, 15388.7)
      val geo = RasterExtent(e, 60.0, 60.0, 513, 513)
      val raster2 = GeoTiffReader.readPath(path, None, Some(geo))
    }

    it ("should draw") {
      val path = "src/test/resources/econic.tif"
      val raster = GeoTiffReader.readPath(path, None, None)

      val (zmin, zmax) = raster.findMinMax

      val chooser = new MultiColorRangeChooser(Array(0xFF0000, 0xFFFF00, 0x0000FF))
      val breaks = (zmin to zmax)
      val colors = chooser.getColors(breaks.length)
      val pairs = breaks.zip(colors).toArray

      val png = io.WritePNGFile(raster, "/tmp/fromgeo.png", pairs, NODATA, false)
      server.run(png)
    }

    it ("should write") {
      val r = GeoTiffReader.readPath("src/test/resources/econic.tif", None, None)
      GeoTiffWriter.write("/tmp/written.tif", r, "foo")
    }

    it ("should write floating point rasters") {
      val e = Extent(100.0, 400.0, 120.0, 420.0)
      val re = RasterExtent(e, 10.0, 10.0, 2, 2)
      val data = DoubleArrayRasterData(Array(11.0, 22.0, 33.0, 44.0), 2, 2)
      val r = Raster(data, re)
      GeoTiffWriter.write("/tmp/float.tif", r, "float")
    }

    it ("should write bennet's geotiff") {
      import geotiff._
      val r = server.run(io.LoadFile("src/test/resources/quadborder8.arg"))
      Encoder.writePath("/tmp/bennet.tif", r, Settings.float64)
    }

    it ("should compress a toy 4x4 raster") {
      import geotiff._

      val cols = 4
      val rows = 4

      val e = Extent(10.0, 20.0, 10.0 + cols, 20.0 + rows)
      val re = RasterExtent(e, 1.0, 1.0, cols, rows)
      val data = ByteArrayRasterData((0 until 80 by 5).map(_.toByte).toArray, 4, 4)
      val r = Raster(data, re)

      Encoder.writePath("/tmp/lzw-4x4.tif", r, Settings(ByteSample, Signed, false, Lzw))
      Encoder.writePath("/tmp/raw-4x4.tif", r, Settings(ByteSample, Signed, false, Uncompressed))
    }

    it ("should compress a large raster") {
      import geotiff._

      val r = server.run(io.LoadFile("src/test/resources/econic.tif"))
      Encoder.writePath("/tmp/lzw-eco.tif", r, Settings(ByteSample, Signed, false, Lzw))
      Encoder.writePath("/tmp/raw-eco.tif", r, Settings(ByteSample, Signed, false, Uncompressed))
    }

    it ("should compress a larger raster") {
      import geotiff._
    
      val r = server.run(io.LoadFile("src/test/resources/sbn/SBN_inc_percap.arg"))
      Encoder.writePath("/tmp/lzw-sbn.tif", r, Settings(ByteSample, Signed, false, Lzw))
      Encoder.writePath("/tmp/raw-sbn.tif", r, Settings(ByteSample, Signed, false, Uncompressed))
    }
  }
}
