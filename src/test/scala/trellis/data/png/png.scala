package trellis.data.png

import scala.io.{Source}
import scala.tools.nsc.io.{PlainFile}

import java.io.{File,FileInputStream}

import trellis._
import trellis.data.PNGRenderer

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

class PNGSpec extends Spec with MustMatchers {
  describe("A PNGRenderer") {
    val width = 256
    val height = 256
    val data   = Array.ofDim[Int](height * width)
    for(row <- 0 until height) {
      for(col <- 0 until width) {
        data(row * width + col) = row * 0xFF + col
      }
    }

    val e = Extent(0.0, 0.0, 100.0, 100.0)
    val rasterExtent = RasterExtent(e, 1.0, 1.0, 100, 100)
    val raster   = IntRaster(data, rasterExtent)

    val fh = File.createTempFile("bar", ".png")

    val bg = 0
    val transparent = true
    val writer = new PNGRenderer(raster, fh.getPath, (z:Int) => z, bg, transparent)
    it("should write a PNG") {
      writer.write
    }

    it("should render a PNG") {
      writer.render
    }

    it("should write and render the same thing") {
      val bytes1 = writer.render

      val fis    = new FileInputStream(fh)
      val bytes2 = Array.ofDim[Byte](fh.length.toInt)
      fis.read(bytes2)

      bytes1 must be === bytes2
    }

    fh.delete()
  }
}
