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

import geotrellis.vector._
import org.apache.commons.math3.stat.regression.SimpleRegression

object LinearSemivariogram {
  def apply(sill: Double, nugget: Double): Double => Double =
    { x: Double => x * sill + nugget }

  def apply(pts: Array[PointFeature[Double]], radius: Option[Double] = None, lag: Double = 0): Semivariogram = {
    // Construct slope and intercept
    val regression = new SimpleRegression
    val empiricalSemivariogram: Seq[(Double, Double)] = EmpiricalVariogram.linear(pts, radius, lag)
    for((x, y) <- empiricalSemivariogram) { regression.addData(x, y) }
    val slope = regression.getSlope
    val intercept = regression.getIntercept
    Semivariogram({ x => slope * x + intercept }, 0, slope, intercept)
  }
}