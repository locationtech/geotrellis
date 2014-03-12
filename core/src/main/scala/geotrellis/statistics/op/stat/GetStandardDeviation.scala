/**************************************************************************
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
 **************************************************************************/

package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics._


/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 *
 * @return        Raster of TypeInt data
 *
 * @note          Currently only supports working with integer types. If you pass in a Raster
 *                with double type data (TypeFloat,TypeDouble) the values will be rounded to
 *                Ints.
 */
case class GetStandardDeviation(r:Op[Raster], h:Op[Histogram], factor:Int) extends Op[Raster] {
  val g = GetStatistics(h)

  def _run() = runAsync(List(g, r))

  val nextSteps:Steps = {
    case (stats:Statistics) :: (raster:Raster) :: Nil => step2(stats, raster)
  }

  def step2(stats:Statistics, raster:Raster):StepOutput[Raster] = {
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
    val output = Raster(outdata, raster.rasterExtent)
    Result(output)
  }
}
