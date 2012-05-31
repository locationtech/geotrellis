package geotrellis.data

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import scala.math.abs

import geotrellis._
import geotrellis.process.TestServer
import geotrellis.raster._
import geotrellis.data._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ArgSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("An ArgReader") {
    val server = TestServer()
    val path1 = "src/test/resources/fake.img8.arg"
    	 
    it("should build a valid raster") {
      val raster = Arg8Reader.readPath(path1, None, None)

      raster.cols must be === 4
      raster.rows must be === 4

      for(y <- 0 until 4) {
        for(x <- 0 until 4) {
          raster.get(x, y) must be === (16 * (3 - y) + x + 1)
        }
      }
    }

    it("should write out args") {
      val raster = Arg8Reader.readPath(path1, None, None)

      val fh = java.io.File.createTempFile("foog", ".arg")
      val path2 = fh.getPath

      Arg8Writer.write(path2, raster, "name")

      val data1 = io.Source.fromFile(path2).mkString
      val data2 = io.Source.fromFile("src/test/resources/fake.img8.arg").mkString

      val base = path2.substring(0, path2.lastIndexOf("."))

      new java.io.File(base + ".arg").delete() must be === true
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
      Arg8Reader.readPath("src/test/resources/quad8.arg", None, Some(re))
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
        println("got:")
        println(result.data.asArray.toList)
        println("\nexpected:")
        println(expect.toList)
      }
      ok must be === true
    }

    val nd = NODATA

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

    //TODO: request region totally outside raster
    it("should handle crazy out-of-bounds requests") {
      dotest(-100.0, -100.0, -10.0, -10.0, 2, 2, Array(nd, nd,
                                                       nd, nd))

      dotest(1000.0, 1000.0, 1200.0, 1200.0, 2, 2, Array(nd, nd,
                                                         nd, nd))
    }
  }
}
