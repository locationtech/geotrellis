package geotrellis.data

import geotrellis.process.TestServer

import geotrellis.Extent
import geotrellis._
import geotrellis.raster._


import java.io.{DataInputStream, FileInputStream}

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import scala.math.{abs}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class Arg32Spec extends Spec with MustMatchers with ShouldMatchers {
  val nd = NODATA

  describe("An Arg32Reader") {
    val server = TestServer()

    val path1 = "src/test/resources/fake.img.arg32"

    it("should build a valid raster") {
      val raster = Arg32Reader.readPath(path1, None, None)
      //val raster = reader.getRaster

      raster.cols must be === 4
      raster.rows must be === 4

      for(y <- 0 until 4) {
        for(x <- 0 until 4) {
          raster.get(x, y) must be === (16 * y + x + 1)
        }
      }
    }

    val raster = Arg32Reader.readPath(path1, None, None)

    it("should write to full paths ") {
      val fh = java.io.File.createTempFile("foog", ".arg32")
      val path = fh.getPath
      val base = path.substring(0, path.length - 6)

      Arg32Writer.write(path, raster)

      val data1 = io.Source.fromFile(path).mkString
      val data2 = io.Source.fromFile("src/test/resources/fake.img.arg32").mkString
    
      new java.io.File(base + ".arg32").delete() must be === true
      new java.io.File(base + ".json").delete() must be === true
      data1 must be === data2
    }

    it("should write out no-data args") {
      val e = Extent(0.0, 0.0, 40.0, 40.0)
      val geo = RasterExtent(e, 10.0, 10.0, 4, 4)
      val data1 = Array.fill[Int](16)(nd)
      val raster = IntRaster(data1, geo)
      
      val fh = java.io.File.createTempFile("nodata", ".arg32")
      val path = fh.getPath
      val base = path.substring(0, path.length - 6)

      Arg32Writer.write(path, raster)

      val data2 = Array.ofDim[Int](16)
      val dis = new DataInputStream(new FileInputStream(path))
      for(i <-0 until 16) { data2(i) = dis.readInt() }
      dis.close

      new java.io.File(path).delete() must be === true
      new java.io.File(base + ".json").delete() must be === true
      data1 must be === data2
    }
    
    // helper function
    val chunker = (xmin:Double, ymin:Double, xmax:Double, ymax:Double,
                   cols:Int, rows:Int) => {
      val cellwidth  = abs(xmax - xmin) / cols
      val cellheight = abs(ymax - ymin) / cols
      val e = Extent(xmin, ymin, xmax, ymax)
      val re = RasterExtent(e, cellwidth, cellheight, cols, rows)

      Arg32Reader.readPath("src/test/resources/quad.arg32", None, Some(re))
    }
    
    // helper function
    val dcmp = (a1:Array[Int], a2:Array[Int]) => {
      if (a1.length == a2.length) {
        (0 until a1.length).foldLeft(true) {
          (bool, i) => bool && (a1(i) == a2(i))
        }
      } else {
        false
      }
    }
    
    // helper function
    val dotest = (xmin:Double, ymin:Double, xmax:Double, ymax:Double,
                  cols:Int, rows:Int, expect:Array[Int]) => {
      val result = chunker(xmin, ymin, xmax, ymax, cols, rows)
      val ok = dcmp(result.data.asArray, expect)
      if (!ok) {
        println(result.asciiDraw)
        println("got:")
        println(result.data.asArray.toList)
        println("\nexpected:")
        println(expect.toList)
      }
      ok must be === true
    }
    
    it("should handle simple chunks") {
      dotest(-9.5, 43.8, 150.5, 123.8, 10, 5, Array(1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
                                                    1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
                                                    3, 3, 3, 3, 3, 4, 4, 4, 4, 4,
                                                    3, 3, 3, 3, 3, 4, 4, 4, 4, 4,
                                                    3, 3, 3, 3, 3, 4, 4, 4, 4, 4))
                                                          
                                                          
    }
    
    it("should handle complex chunks (#1)") {
      dotest(1.0, -48.5, 81.0, 31.5, 4, 4, Array(3, 3, 3, 4,
                                                 nd, nd, nd, nd,
                                                 nd, nd, nd, nd,
                                                 nd, nd, nd, nd))
    }
    
    it("should handle complex chunks (#2)") {
      dotest(30.5, 3.8, 150.5, 123.8, 8, 8, Array(1, 1, 1, 2, 2, 2, 2, 2,
                                                  1, 1, 1, 2, 2, 2, 2, 2,
                                                  1, 1, 1, 2, 2, 2, 2, 2,
                                                  3, 3, 3, 4, 4, 4, 4, 4,
                                                  3, 3, 3, 4, 4, 4, 4, 4,
                                                  3, 3, 3, 4, 4, 4, 4, 4,
                                                  3, 3, 3, 4, 4, 4, 4, 4,
                                                  3, 3, 3, 4, 4, 4, 4, 4))
    }
    
    //TODO: request region totally outside raster
    it("should handle crazy out-of-bounds requests") {
      dotest(-100.0, -100.0, -10.0, -10.0, 2, 2, Array(nd, nd,
                                                       nd, nd))
    
      dotest(1000.0, 1000.0, 1200.0, 1200.0, 2, 2, Array(nd, nd,
                                                         nd, nd))
    }
  }
}
