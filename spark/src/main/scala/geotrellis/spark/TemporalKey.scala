package geotrellis.spark

import geotrellis.spark.io.json.Implicits._

import com.github.nscala_time.time.Imports._
import spray.json._

object TemporalKey {
  def apply(dateTime: DateTime): TemporalKey =
    TemporalKey(dateTime.getMillis)

  implicit def dateTimeToKey(time: DateTime): TemporalKey =
    TemporalKey(time)

  implicit def keyToDateTime(key: TemporalKey): DateTime =
    key.time

  implicit def ordering[A <: TemporalKey]: Ordering[A] =
    Ordering.by(tk => tk.instant)

}

/** A TemporalKey designates the temporal positioning of a layer's tile. */
case class TemporalKey(instant: Long) {
  def time: DateTime = new DateTime(instant, DateTimeZone.UTC)
}
