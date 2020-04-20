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

import cats.syntax.foldable._
import cats.syntax.option._
import cats.instances.list._
import cats.instances.either._
import org.log4s.getLogger

/**
  * Options ported from GDAL (https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-ovr)
  *  for automatic selection from among available overviews
  */
sealed trait OverviewStrategy

object OverviewStrategy {
  @transient private[this] lazy val logger = getLogger

  def DEFAULT: OverviewStrategy = AutoHigherResolution

  /**
    * Select appropriate overview given the strategy.
    *
    * WARN: this function assumes that CellSizes are sorted by their resolution, with "smaller" cell sizes appearing first.
    * It interprets idx 0 as the position with the highest CellSize.  If the input list is not sorted, this function returns
    * idx 0.
    *
    * Unless a particular strategy suggests otherwise, this method will clamp the returned
    * index to the range of overviewCS.
    * @param overviewCS a list of CellSizes sorted by `resolution`, result is undefined for unsorted lists.
    * @param desiredCS  cellSize that would be searched
    * @param strategy   overview strategy used to search for the best matching CellSize
    * @return           index of the closest cellSize. Each strategy defines a different behavior:
    *                   Level: selects the passed index and if it is OOB clamps the index.
    *                   Auto(n): selects the best matching cellSize (approximately) and adds n, requires the input resolutions list to be sorted.
    *                   Base: always returns the best matching cellSize from the given list of CellSizes.
    *                   AutoHigherResolution: selects the best matching cellSize (approximately) always selects a higher resolution,
    *                   requires the input resolutions list to be sorted.
    */
  def selectOverview(
    overviewCS: List[CellSize],
    desiredCS: CellSize,
    strategy: OverviewStrategy
  ): Int = {
    val maybeIndex = strategy match {
      case Level(overviewIdx) => overviewIdx
      case Auto(n)            => selectIndexByProximity(overviewCS, desiredCS, 0.5) + n
      // returns the best resolution always
      case Base               => overviewCS.indexOf(overviewCS.min)
      // Returns -1 in case nothing was found or the input list was not sorted.
      case AutoHigherResolution =>
        val idx = selectIndexByProximity(overviewCS, desiredCS, 1.0)
        // AutoHigherResolution defaults to the closest level
        // idx > overviewCS.size means, that idx = overviewCS.size (last idx + 1)
        if (idx > overviewCS.size) idx - 1 else idx
    }
    // clamp the index to fit the input list size
    if (maybeIndex < 0) 0
    else if (maybeIndex >= overviewCS.size) overviewCS.size - 1
    else maybeIndex
  }

  /**
    * This method is unsafe. It's up to the selectOverview method to handle each index OOB error.
    * @param overviewCS         a sorted list of resolutions, otherwise it is not guaranteed that it would work with all the input strategies.
    * @param desiredCS          cellSize that would be searched
    * @param proximityThreshold threshold to defined the search proximity
    * @return                   index of the closest cellSize. Returns -1 in case nothing was found or the input list was not sorted.
    */
  private def selectIndexByProximity(overviewCS: List[CellSize], desiredCS: CellSize, proximityThreshold: Double): Int =
    overviewCS.zipWithIndex.foldLeftM(Option.empty[(CellSize, Int)]) { case (acc, r @ (rightCZ, _)) =>
      acc match {
        case l @ Some((leftCZ, _)) =>
          val proportion = 1 - (rightCZ.resolution - desiredCS.resolution) / (rightCZ.resolution - leftCZ.resolution)

          // if the rightCZ > than the leftCZ it means that the list is not sorted
          // - negative if rightCZ < leftCZ
          // - positive if rightCZ > leftCZ
          // - zero otherwise (if rightCZ == leftCZ)
          if (Ordering[CellSize].compare(rightCZ, leftCZ) < 0) {
            logger.debug(s"The input list $overviewCS is probably not sorted.")
            Left(none)
          }
          // if proportion >= proximityThreshold continue the search else stop
          else if (proportion >= proximityThreshold) Right(r.some) else Left(l)
        case None => Right(r.some)
      }
    } match {
      case Right(Some((_, idx))) => idx
      case Left(Some((_, idx)))  => idx
      // nothing was found
      case _ => -1
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
