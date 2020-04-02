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

import geotrellis.raster.CellSize

import scala.collection.Searching._

/**
  * Options ported from GDAL (https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-ovr)
  *  for automatic selection from among available overviews
  */
sealed trait OverviewStrategy

object OverviewStrategy {
  def DEFAULT = AutoHigherResolution

  /**
    * Select appropriate overview given the strategy.
    *
    * WARN: this function assumes that CellSizes are sorted. It interprets idx 0 as the position with the highest CellSize.
    *
    * Unless a particular strategy suggests otherwise, this method will clamp the returned
    * index to the range of overviewCS.
    * @param overviewCS
    * @param desiredCS
    * @param strategy
    * @return
    */
  def selectOverview(
    overviewCS: List[CellSize],
    desiredCS: CellSize,
    strategy: OverviewStrategy
  ): Int = {
    val maybeIndex = strategy match {
      case Level(overviewIdx) =>
        overviewIdx
      case Auto(n) =>
        selectIndexByProximity(overviewCS, desiredCS, 0.5) + n
      case Base =>
        overviewCS.indexOf(overviewCS.min)
      case AutoHigherResolution =>
        val idx = selectIndexByProximity(overviewCS, desiredCS, 1.0)
        // AutoHigherResolution defaults to the closest level
        // idx > overviewCS.size means, that idx = overviewCS.size (last idx + 1)
        if (idx > overviewCS.size) idx - 1 else idx
    }
    if (maybeIndex < 0) 0
    else if (maybeIndex >= overviewCS.size) overviewCS.size - 1
    else maybeIndex
  }

  // This method is unsafe. It's up to the selectOverview method to handle each index OOB error for
  // each indivdual strategy
  private def selectIndexByProximity(overviewCS: List[CellSize], desiredCS: CellSize, proximityThreshold: Double): Int =
    overviewCS.search(desiredCS) match {
      case InsertionPoint(ipIdx) =>
        if (ipIdx <= 0 || ipIdx >= overviewCS.length) {
          ipIdx
        } else {
          val left = overviewCS(ipIdx - 1)
          val right = overviewCS(ipIdx)
          val proportion =
          1 - (right.resolution - desiredCS.resolution) / (right.resolution - left.resolution)
          if (proportion >= proximityThreshold) ipIdx
          else ipIdx - 1
        }
      case Found(csIdx) =>
        csIdx
  }
}

/**
  * If the index of the overview from which data should be sampled is known, it can
  *  be explicitly provided via this option
  */
case class Level(overviewLevel: Int) extends OverviewStrategy

/**
  * While n=0, the nearest zoom level should be selected. At n=1, the
  * overview immediately after n=0 is selected, at n=2, the one after that etc.
  * 
  * @note n must be greater than or equal to 0
  */
case class Auto(n: Int = 0) extends OverviewStrategy {
  require(n >= 0, s"n should be 0 or positive, given n is: $n")
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
