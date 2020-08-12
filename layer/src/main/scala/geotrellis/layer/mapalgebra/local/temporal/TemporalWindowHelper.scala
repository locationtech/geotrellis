/*
 * Copyright 2019 Azavea
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

package geotrellis.layer.mapalgebra.local.temporal




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
