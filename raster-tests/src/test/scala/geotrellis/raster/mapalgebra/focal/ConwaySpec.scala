/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ConwaySpec extends AnyFunSpec with FocalOpSpec with Matchers {

  val getConwayResult =
    Function.uncurried(
      (getCellwiseResult _)
        .curried{(r,n) => Conway.calculation(r,n)}(Square(1))
    )

  val calc = Conway
  describe("Conway's Game of Life") {
    it("should compute death by overpopulation") {
      val s = Seq[Int](1,1,1,1,NODATA,NODATA,NODATA)
      getConwayResult(s,Seq[Int]()) should equal (NODATA)
    }

    it("should compute death by underpopulation") {
      val s = Seq[Int](1,NODATA,NODATA,NODATA,NODATA,NODATA)
      getConwayResult(s,Seq[Int]()) should equal (NODATA)
    }

    it("should let them live if they be few but merry") {
      val s = Seq[Int](1,1,1,NODATA,NODATA,NODATA,NODATA)
      getConwayResult(s,Seq[Int]()) should equal (1)
      val s2 = Seq[Int](1)
      getConwayResult(s,s2) should equal (1)
    }

    it("should let them live if they let too many neighbors die") {
      val s = Seq[Int](1,1,1,NODATA,NODATA,NODATA,NODATA)
      val s2 = Seq[Int](1,1)
      getConwayResult(s,s2) should equal (NODATA)
    }
  }
}
