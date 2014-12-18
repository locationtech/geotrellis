package geotrellis.spark.op.local.temporal

import geotrellis.spark._

import org.joda.time.{DateTimeZone, DateTime}

import reflect.ClassTag

object TemporalWindowHelper {

  val Seconds = 1
  val Minutes = 2
  val Hours = 3
  val Days = 4
  val Weeks = 5
  val Months = 6
  val Years = 7

  val Average = 1
  val Minimum = 2
  val Maximum = 3
  val Mode = 4

  def badState = throw new IllegalStateException("Bad temporal window method state.")

  def parseUnit(s: String) = s.toLowerCase match {
    case "seconds" => Seconds
    case "minutes" => Minutes
    case "hours" => Hours
    case "days" => Days
    case "weeks" => Weeks
    case "months" => Months
    case "years" => Years
    case _ => throw new IllegalArgumentException("Unknown unit: $s.")
  }

}

case class TemporalWindowState[K](
  rasterRDD: RasterRDD[K],
  method: Int,
  periodStep: Option[Int] = None,
  unit: Option[Int] = None
)(
  implicit val keyClassTag: ClassTag[K],
    _sc: SpatialComponent[K],
    _tc: TemporalComponent[K]) {

  import TemporalWindowHelper._

  private lazy val state =
    if (periodStep.isEmpty && unit.isEmpty) 0
    else 2

  def per(p: Int)(unitString: String): TemporalWindowState[K] =
    if (state != 0) badState
    else {
      val u = parseUnit(unitString)
      copy(periodStep = Some(p), unit = Some(u))
    }

  def from(start: DateTime): RasterRDD[K] =
    if (state != 2) badState
    else method match {
      case Average => ???
      case Minimum => rasterRDD.temporalMin(periodStep.get, unit.get, start)
      case Maximum => rasterRDD.temporalMax(periodStep.get, unit.get, start)
      case Mode => ???
      case _ => throw new IllegalStateException("Bad method $method.")
    }

}
