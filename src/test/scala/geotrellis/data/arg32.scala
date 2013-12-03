package geotrellis.data.arg

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.testutil._

import java.io.{DataInputStream, FileInputStream}

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import scala.math.abs

class Arg32Spec extends FunSpec 
                   with MustMatchers 
                   with ShouldMatchers {
  val nd = NODATA

  describe("An Arg32Reader") {

    val path1 = "src/test/resources/fake.img32.json"

    it("should use correct no data values") {

      import geotrellis.data.arg
      new Int8ReadState(Left(""), null, null).getNoDataValue must be === -128
      new Int16ReadState(Left(""), null, null).getNoDataValue must be === -32768
      new Int32ReadState(Left(""), null, null).getNoDataValue must be === -2147483648
    }

    it("should build a valid raster") {
      val raster = RasterLayer.fromPath(path1).get.getRaster()

      raster.cols must be === 4
      raster.rows must be === 4

      for(y <- 0 until 4) {
        for(x <- 0 until 4) {
          raster.get(x, y) must be === (16 * (3 - y) + x + 1)
        }
      }
    }

    println(new java.io.File(path1).getAbsolutePath)
    val raster = RasterLayer.fromPath(path1).get.getRaster()

    it("should write to full paths ") {
      val fh = java.io.File.createTempFile("foog", ".arg")
      val path = fh.getPath

      ArgWriter(TypeInt).write(path, raster, "foog")

      val data1 = scala.io.Source.fromFile(path).mkString
      val data2 = scala.io.Source.fromFile("src/test/resources/fake.img32.arg").mkString
    
      //new java.io.File(base + ".arg").delete() must be === true
      //new java.io.File(base + ".json").delete() must be === true
      data1 must be === data2
    }

    it("should allow file extension to be excluded") {
      val fh = java.io.File.createTempFile("testraster2", ".arg")
      val path = fh.getPath
      val base = path.substring(0, path.length - 4)
      ArgWriter(TypeInt).write(base, raster, "foog2")

      val data1 = scala.io.Source.fromFile(path).mkString
      val data2 = scala.io.Source.fromFile("src/test/resources/fake.img32.arg").mkString
      data1 must be === data2
    }

    it("should write out no-data args") {
      val e = Extent(0.0, 0.0, 40.0, 40.0)
      val geo = RasterExtent(e, 10.0, 10.0, 4, 4)
      val data1 = Array.fill[Int](16)(nd)
      val raster = Raster(data1, geo)
      
      val fh = java.io.File.createTempFile("nodata", ".arg")
      val path = fh.getPath
      val base = path.substring(0, path.length - 4)

      ArgWriter(TypeInt).write(path, raster, "nodata")

      val data2 = Array.ofDim[Int](16)
      val dis = new DataInputStream(new FileInputStream(path))
      for (i <- 0 until 16) data2(i) = dis.readInt()
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

      RasterLayer.fromPath("src/test/resources/quad32.json").get.getRaster(Some(re))
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
      val d = result.toArray
      val ok = dcmp(d, expect)
      if (!ok) {
        println(result.asciiDraw)
        println("got:")
        println(d.toList)
        println("\nexpected:")
        println(expect.toList)
      }
      ok must be === true
    }
    
    it("should handle simple chunks") {
      dotest(-9.5, 43.8, 150.5, 123.8, 10, 5, Array(1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
                                                    1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
                                                    1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
                                                    3, 3, 3, 3, 3, 4, 4, 4, 4, 4,
                                                    3, 3, 3, 3, 3, 4, 4, 4, 4, 4))
                                                          
                                                          
    }
    
    it("should handle complex chunks (#1)") {
      dotest(1.0, -48.5, 81.0, 31.5, 4, 4, Array(3, 3, 3, 4,
                                                 3, 3, 3, 4,
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
    
    it("should handle crazy out-of-bounds requests") {
      dotest(-100.0, -100.0, -10.0, -10.0, 2, 2, Array(nd, nd,
                                                       nd, nd))
    
      dotest(1000.0, 1000.0, 1200.0, 1200.0, 2, 2, Array(nd, nd,
                                                         nd, nd))
    }
  }
}
