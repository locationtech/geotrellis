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

package geotrellis.spark.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.spark._

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

import java.time.ZonedDateTime
import reflect.ClassTag

object TemporalWindowHelper {

  val UnitSeconds = 1
  val UnitMinutes = 2
  val UnitHours = 3
  val UnitDays = 4
  val UnitWeeks = 5
  val UnitMonths = 6
  val UnitYears = 7

  val Average = 1
  val Minimum = 2
  val Maximum = 3
  val Variance = 4

  def badState = throw new IllegalStateException("Bad temporal window method state.")

  def parseUnit(s: String) = s.toLowerCase match {
    case "seconds" => UnitSeconds
    case "minutes" => UnitMinutes
    case "hours" => UnitHours
    case "days" => UnitDays
    case "weeks" => UnitWeeks
    case "months" => UnitMonths
    case "years" => UnitYears
    case _ => throw new IllegalArgumentException("Unknown unit: $s.")
  }

}

case class TemporalWindowState[K](
  rdd: RDD[(K, Tile)],
  method: Int,
  windowSize: Option[Int] = None,
  unit: Option[Int] = None,
  start: Option[ZonedDateTime] = None,
  partitioner: Option[Partitioner] = None
)(
  implicit val keyClassTag: ClassTag[K],
    _sc: SpatialComponent[K],
    _tc: TemporalComponent[K]) {

  import TemporalWindowHelper._

  private lazy val state =
    if (windowSize.isEmpty && unit.isEmpty) 0
    else if (start.isEmpty) 1
    else 2

  def per(p: Int)(unitString: String): TemporalWindowState[K] =
    if (state != 0) badState
    else {
      val u = parseUnit(unitString)
      copy(windowSize = Some(p), unit = Some(u))
    }

  def from(s: ZonedDateTime): TemporalWindowState[K] =
    if (state != 1) badState
    else copy(start = Some(s))

  def to(to: ZonedDateTime) =
    if (state != 2) badState
    else method match {
      case Average => rdd.temporalMean(windowSize.get, unit.get, start.get, to, partitioner)
      case Minimum => rdd.temporalMin(windowSize.get, unit.get, start.get, to, partitioner)
      case Maximum => rdd.temporalMax(windowSize.get, unit.get, start.get, to, partitioner)
      case Variance => rdd.temporalVariance(windowSize.get, unit.get, start.get, to, partitioner)
      case _ => throw new IllegalStateException("Bad method $method.")
    }

}
