/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.data

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class ExtentSpec extends FunSpec 
                    with MustMatchers 
                    with ShouldMatchers 
                    with TestServer {

  def re(pt:(Double, Double), cs:(Double, Double), ncols:Int, nrows:Int) = {
    val (x1, y1) = pt
    val (cw, ch) = cs
    val e = Extent(x1, y1, x1 + ncols * cw, y1 + nrows * ch)
    RasterExtent(e, cw, ch, ncols, nrows)
  }

  def load(name:String) = io.LoadRaster(name)
  def load(name:String, pt:(Double, Double), cs:(Double, Double), ncols:Int, nrows:Int) = {
    io.LoadRaster(name, re(pt, cs, ncols, nrows))
  }

  def xload(path:String) = io.LoadFile(path)
  def xload(path:String, pt:(Double, Double), cs:(Double, Double), ncols:Int, nrows:Int) = {
    io.LoadFile(path, re(pt, cs, ncols, nrows))
  }

  describe("An Extent") {
    def confirm(op:Op[Raster], expected:Array[Int]) {
      val r = get(op)
      r.toArray should be (expected)
    }

    it("should load as expected") {

      val origin1 = (1100.0, 1200.0)
      val cellSizes1 = (100.0, 100.0)

      val name = "6x6int8"
      val path = "src/test/resources/data/6x6int8.arg"

      val expected1 = (1 to 36).toArray

      val expected23 = Array(0x08, 0x09, 0x0A,
        0x0E, 0x0F, 0x10,
        0x14, 0x15, 0x16)

      val expected4 = Array(0x0E, 0x0F,
        0x14, 0x15)

      val op1 = load(name)
      val op2 = load(name, origin1, cellSizes1, 3, 3)
      confirm(op1, expected1)
      confirm(op2, expected23)

      val xop1 = xload(path)
      val xop2 = xload(path, origin1, cellSizes1, 3, 3)
      confirm(xop1, expected1)
      confirm(xop2, expected23)
    }
  }
}
