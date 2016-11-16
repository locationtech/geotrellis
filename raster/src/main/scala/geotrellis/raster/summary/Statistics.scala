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

package geotrellis.raster.summary

import geotrellis.raster.{doubleNODATA, NODATA}


/**
  * Data object for sharing the basic statistics about a raster or
  * region.
  */
case class Statistics[@specialized (Int, Double) T]
(
  dataCells: Long = 0,
  mean: Double = Double.NaN,
  median: T,
  mode: T,
  stddev: Double = Double.NaN,
  zmin: T,
  zmax: T
)

/**
  * Companion object for the [[Statistics]] type.  Contains functions
  * for generating empty Statistics objects.
  */
object Statistics {

  /**
    * Empty integer [[Statistics]].
    */
  def EMPTYInt() = {
    Statistics[Int](
      dataCells = 0,
      mean = Double.NaN,
      median = NODATA,
      mode = NODATA,
      stddev = Double.NaN,
      zmin = NODATA,
      zmax = NODATA
    )
  }

  /**
    * Empty double [[Statistics]].
    */
  def EMPTYDouble() = {
    Statistics[Double](
      dataCells = 0,
      mean = Double.NaN,
      median = doubleNODATA,
      mode = doubleNODATA,
      stddev = Double.NaN,
      zmin = doubleNODATA,
      zmax = doubleNODATA
    )
  }
}
