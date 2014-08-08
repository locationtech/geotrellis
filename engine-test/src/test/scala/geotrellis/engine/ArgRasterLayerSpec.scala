/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.engine

import geotrellis.raster._
import geotrellis.raster.io.arg._
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

import scala.math.abs

class ArgRasterLayerSpec extends FunSpec 
                            with Matchers 
                            with TestEngine 
                            with TileBuilders {
  describe("A RasterLayer") {
    val path1 = "raster-test/data/fake.img8.json"
    	 
    it("should build a valid raster") {
      val raster = RasterLayer.fromPath(path1).get.getRaster()

      raster.cols should be (4)
      raster.rows should be (4)

      for(y <- 0 until 4) {
        for(x <- 0 until 4) {
          raster.get(x, y) should be (16 * (3 - y) + x + 1)
        }
      }
    }

    it("should write out args") {
      val layer = RasterLayer.fromPath(path1).get
      val raster = layer.getRaster()
      val rasterExtent = layer.info.rasterExtent

      val fh = java.io.File.createTempFile("foog", ".arg")
      val path2 = fh.getPath

      ArgWriter(TypeByte).write(path2, raster, rasterExtent.extent, "name")

      val data1 = scala.io.Source.fromFile(path2).mkString
      val data2 = scala.io.Source.fromFile("raster-test/data/fake.img8.arg").mkString

      val base = path2.substring(0, path2.lastIndexOf("."))

      new java.io.File(base + ".arg").delete() should be (true)
      new java.io.File(base + ".json").delete() should be (true)
      data1 should be (data2)
    }

    // helper function
    val chunker = (xmin:Double, ymin:Double, xmax:Double, ymax:Double,
                   cols:Int, rows:Int) => {
      val cellwidth  = abs(xmax - xmin) / cols
      val cellheight = abs(ymax - ymin) / rows
      val e = Extent(xmin, ymin, xmax, ymax)
      val re = RasterExtent(e, cellwidth, cellheight, cols, rows)
      RasterLayer.fromPath("raster-test/data/quad8.json").get.getRaster(Some(re))
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
      println(result.asciiDraw)
      val d = result.toArray
      val ok = dcmp(d, expect)
      if (!ok) {
        println("got:")
        println(d.toList)
        println("\nexpected:")
        println(expect.toList)
      }
      ok should be (true)
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

    it("should handle crazy out-of-bounds requests") {
      dotest(-100.0, -100.0, -10.0, -10.0, 2, 2, Array(nd, nd,
                                                       nd, nd))

      dotest(1000.0, 1000.0, 1200.0, 1200.0, 2, 2, Array(nd, nd,
                                                         nd, nd))

      dotest(110000.0, 120000.0, 121000.0, 122000.0, 2, 2, Array(nd, nd,
                                                                 nd, nd))
    }
  }
}
