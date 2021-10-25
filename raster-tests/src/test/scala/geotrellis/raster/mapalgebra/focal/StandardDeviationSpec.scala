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

class StandardDeviationSpec extends AnyFunSpec with FocalOpSpec with Matchers {
  val getCircleStdResult = (getDoubleCursorResult _).curried(
    (r,n) => StandardDeviation.calculation(r,n)
  )(Circle(1))

  val getSquareStdResult = (getDoubleCursorResult _).curried(
    (r,n) => StandardDeviation.calculation(r,n)
  )(Square(1))

  def mean(xs: List[Int]): Double = xs match {
    case Nil => Double.NaN
    case ys => ys.sum / ys.size.toDouble
  }
                                              
  def stddev(xs: List[Int], avg: Double): Double = xs match {
    case Nil => Double.NaN
    case ys => math.sqrt(ys.foldLeft(0.0) {
      (a,e) => a + math.pow(e - avg, 2.0)
    } / xs.size.toDouble)
  }

  describe("StandardDeviation") {
    it("should handle all NODATA") {
      isNoData(getSquareStdResult(MockCursor.fromAll(NODATA,NODATA,NODATA,NODATA))) should be (true)
    }

    it("should match calculated std on default sets") {
      for(s <- defaultTestSets) {
        val sf = s.filter { x => isData(x) }.toList
        val xs = sf
        val μ = mean(xs)
        val σ = stddev(xs, μ)
        if(isNoData(σ)) {
          isNoData(getSquareStdResult(MockCursor.fromAddRemoveAll(s,s,Seq[Int]()))) should equal (true)
        } else {
          getSquareStdResult(MockCursor.fromAddRemoveAll(s,s,Seq[Int]())) should equal (σ)
        }
      }
    }
  }
}
