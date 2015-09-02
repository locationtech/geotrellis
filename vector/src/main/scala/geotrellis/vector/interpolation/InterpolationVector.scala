/*
* Copyright (c) 2015 Azavea.
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

package geotrellis.vector.interpolation

import geotrellis.vector.Point
import spire.syntax.cfor._

object InterpolationVector {

  def apply(pointArr: Array[Point])(predictor: (Double, Double) => (Double, Double)): Array[(Double, Double)] = {
    val result = Array.ofDim[(Double, Double)](pointArr.length)

    cfor(0)(_ < pointArr.length, _ + 1) { i: Int =>
      result(i) = predictor(pointArr(i).x, pointArr(i).y)
    }
    result
  }

  def kriging(pointArr: Array[Point])(kriging: Kriging): Array[(Double, Double)] = {
    apply(pointArr) { (x: Double, y: Double) => kriging(x, y) }
  }
}
