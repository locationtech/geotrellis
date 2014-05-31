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

package geotrellis.raster.statistics.op.stat

import geotrellis._
import geotrellis.raster.statistics._

/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 *
 * @return        Tile of TypeInt data
 *
 * @note          Currently only supports working with integer types. If you pass in a Tile
 *                with double type data (TypeFloat, TypeDouble) the values will be rounded to
 *                Ints.
 */
case class GetStandardDeviation(r: Op[Tile], h: Op[Histogram], factor: Int) extends Op[Tile] {
  val g = GetStatistics(h)

  def _run() = runAsync(List(g, r))

  val nextSteps: Steps = {
    case (stats: Statistics) :: (raster: Tile) :: Nil => step2(stats, raster)
  }

  def step2(stats: Statistics, raster: Tile): StepOutput[Tile] = {
    val indata = raster.toArray
    val len = indata.length
    val outdata = Array.ofDim[Int](len)

    val mean = stats.mean
    val stddev = stats.stddev

    var i = 0
    while (i < len) {
      val delta = indata(i) - mean
      outdata(i) = (delta * factor / stddev).toInt
      i += 1
    }
    
    Result(Tile(outdata, raster.cols, raster.rows))
  }
}
