/***
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
 ***/

package geotrellis.render.op

import geotrellis._
import geotrellis.render._
import geotrellis.statistics.Histogram

import scala.math.round

// case class BuildColorMapper(colorBreaks:Op[ColorBreaks], noDataColor:Op[Int])
//      extends Op2(colorBreaks, noDataColor)({
//        (bs, c) => Result(ColorMapper(bs, c))
// })

case class BuildColorBreaks(breaks:Op[Array[Int]], colors:Op[Array[Int]])
     extends Op2(breaks, colors)({
       (bs, cs) => Result(ColorBreaks.assign(bs, cs))
})

/**
 * Generate quantile class breaks with assigned colors.
 */
case class GetColorBreaks(h:Op[Histogram], cs:Op[Array[Int]])
     extends Op[ColorBreaks] {

  def _run() = runAsync(List(h, cs))

  val nextSteps:Steps = {
    case (histogram:Histogram) :: (colors:Array[_]) :: Nil => {
      step2(histogram, colors.asInstanceOf[Array[Int]])
    }
  }

  def step2(histogram:Histogram, colors:Array[Int]) = {
    val limits = histogram.getQuantileBreaks(colors.length)
    Result(ColorBreaks.assign(limits, colors))
  }
}

/**
 * Creates a range of colors interpolated from a smaller set of colors.
 */
case class GetColorsFromPalette(palette:Op[Array[Int]], num:Op[Int])
     extends Op2(palette, num)({
       (palette, num) =>
         Result(Color.chooseColors(palette, num))
})
