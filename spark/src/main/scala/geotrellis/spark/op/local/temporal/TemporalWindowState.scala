package geotrellis.spark.op.local.temporal

import geotrellis.spark._

import org.joda.time.{DateTimeZone, DateTime}

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
  rasterRDD: RasterRDD[K],
  method: Int,
  windowSize: Option[Int] = None,
  unit: Option[Int] = None,
  start: Option[DateTime] = None
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

  def from(s: DateTime): TemporalWindowState[K] =
    if (state != 1) badState
    else copy(start = Some(s))

  def to(to: DateTime): RasterRDD[K] =
    if (state != 2) badState
    else method match {
      case Average => rasterRDD.temporalMean(windowSize.get, unit.get, start.get, to)
      case Minimum => rasterRDD.temporalMin(windowSize.get, unit.get, start.get, to)
      case Maximum => rasterRDD.temporalMax(windowSize.get, unit.get, start.get, to)
      case Variance => rasterRDD.temporalVariance(windowSize.get, unit.get, start.get, to)
      case _ => throw new IllegalStateException("Bad method $method.")
    }

}
