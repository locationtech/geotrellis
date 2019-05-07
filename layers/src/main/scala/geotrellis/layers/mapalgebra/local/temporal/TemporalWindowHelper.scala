package geotrellis.layers.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.tiling._

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
