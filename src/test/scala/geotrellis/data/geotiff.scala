package geotrellis.data

import geotrellis.process.TestServer
import geotrellis._
import geotrellis.raster._

import geotrellis.operation._

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

      import geotrellis.operation.render.png.WritePNGFile
      val png = WritePNGFile(raster, "/tmp/fromgeo.png", pairs, NODATA, false)
      server.run(png)
    }

    it ("should write") {
      val inpath = "src/test/resources/econic.tif"
      val raster = GeoTiffReader.readPath(inpath, None, None)
      /*println(raster.asciiDrawRange(0,5,0,5))
      println(raster.get(0,3))
      println(raster.get(1,3))
      println(raster.get(2,3))
      println(raster.rasterExtent)
      */
      val name = "foo"
      val outpath = "/tmp/written.tif"
      GeoTiffWriter.write(outpath, raster, name)
    }

    it ("should write floating point rasters") {
      val e = Extent(100.0, 400.0, 120.0, 420.0)
      val re = RasterExtent(e, 10.0, 10.0, 2, 2)
      val data = DoubleArrayRasterData(Array(11.0, 22.0, 33.0, 44.0))
      val r = Raster(data, re)
      GeoTiffWriter.write("/tmp/float.tif", r, "float")
    }
  }
}
