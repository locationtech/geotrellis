/*
 * Copyright 2018 Azavea
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

package geotrellis.raster.io.geotiff

sealed trait OverviewStrategy
/**
  * Specify Auto-n where n is an integer greater or equal to 0,
  * to select an overview level below the AUTO one (of a higher or equal resolution).
  */
case class Auto(n: Int = 0) extends OverviewStrategy {
  require(n >= 0, s"n should be positive as it's index in the list of overviews, given n is: $n")
}
/**
  * Selects the best matching overview where overview resolution would be higher or equal to desired
  * to prevent data loss, it is the Default strategy.
  * Chooses the base layer if there would be no good enough overview.
  */
case object AutoHigherResolution extends OverviewStrategy
/**
  * Force the base resolution to be used.
  */
case object Base extends OverviewStrategy
