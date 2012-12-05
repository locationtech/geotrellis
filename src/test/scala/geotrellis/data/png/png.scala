package geotrellis.data.png

import scala.io.{Source}

import java.io.{File,FileInputStream}

import geotrellis._
import geotrellis.data.png._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

class PNGSpec extends FunSpec with MustMatchers {

  def parseHexByte(s:String) = Integer.parseInt(s, 16).toByte
  def parseHexBytes(cs:Array[String]) = cs.map(parseHexByte _)
  def parseHexString(s:String) = parseHexBytes(s.trim.replace("\n", " ").split(" "))

  val rgb = parseHexString("""
89 50 4e 47 0d 0a 1a 0a 00 00 00 0d 49 48 44 52
00 00 00 0a 00 00 00 0a 08 02 00 00 00 02 50 58
ea 00 00 00 06 74 52 4e 53 00 00 00 00 00 00 6e
a6 07 91 00 00 00 3c 49 44 41 54 78 01 7d cc 59
0e 00 20 08 03 d1 92 f4 fe 57 46 8c c6 05 8b 84
bf 37 40 f4 b1 6a 09 38 2c 58 0f 61 fe 29 06 c7
a9 fe b1 58 17 27 8b 22 71 2e 5e be 0a c9 bb a8
78 16 0d 5b 6c 11 3b 06 db 3d 95 00 00 00 00 49
45 4e 44 ae 42 60 82""")

  val rgba = parseHexString("""
89 50 4e 47 0d 0a 1a 0a 00 00 00 0d 49 48 44 52
00 00 00 0a 00 00 00 0a 08 06 00 00 00 8d 32 cf
bd 00 00 00 42 49 44 41 54 78 01 85 ce 5b 0a 00
20 08 44 d1 11 dc ff 96 4b 2d e8 a5 93 20 fe 1c
2e 2a 80 06 88 1d be 3a 9c 23 3e 0a b1 60 44 39
9e d0 6b fe 41 8d 37 c8 f1 05 6b 9c c0 1c 17 f0
c5 04 9e f8 03 17 ee 04 00 12 3a 47 85 ae b3 00
00 00 00 49 45 4e 44 ae 42 60 82""")

  val w = 10
  val h = 10
  val data = Array.ofDim[Int](h * w)
  for(row <- 0 until h) {
    for(col <- 0 until w) {
      data(row * w + col) = ((row * 0xFF + col) << 8) + (255)
    }
  }

  val e = Extent(0.0, 0.0, w.toDouble, h.toDouble)
  val rasterExtent = RasterExtent(e, 1.0, 1.0, w, h)
  val raster = Raster(data, rasterExtent)

  describe("RgbaEncoder") {
    val fh = File.createTempFile("rgba-", ".png")
    val path = fh.getPath

    val encoder = Encoder(Settings(Rgba, PaethFilter))

    it("should render a PNG") {
      encoder.writeByteArray(raster)
    }

    it("should write a PNG") {
      encoder.writePath(path, raster)
    }
    
    it("should write and render the same thing") {
      val bytes1 = encoder.writeByteArray(raster)
    
      val fis = new FileInputStream(fh)
      val bytes2 = Array.ofDim[Byte](fh.length.toInt)
      fis.read(bytes2)
      
      bytes1 must be === bytes2
    }
    
    it("should match the reference") {
      val bytes1 = encoder.writeByteArray(raster)
      bytes1 must be === rgba
    }

    fh.delete()
  }

  describe("RgbEncoder") {
    val fh = File.createTempFile("rgb-", ".png")
    val path = fh.getPath
  
    val encoder = Encoder(Settings(Rgb(0), PaethFilter))
  
    // map RGBA values into RGB
    val raster2 = raster.map(_ >> 8)
  
    it("should write a PNG") {
      encoder.writePath(path, raster2)
    }
  
    it("should render a PNG") {
      encoder.writeByteArray(raster2)
    }
  
    it("should write and render the same thing") {
      val bytes1 = encoder.writeByteArray(raster2)
  
      val fis = new FileInputStream(fh)
      val bytes2 = Array.ofDim[Byte](fh.length.toInt)
      fis.read(bytes2)
      
      bytes1 must be === bytes2
    }
  
    it("should match the reference") {
      val bytes1 = encoder.writeByteArray(raster2)
      bytes1 must be === rgb
    }
  
    fh.delete()
  }
}
