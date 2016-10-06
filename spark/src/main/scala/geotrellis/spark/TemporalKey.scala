package geotrellis.spark

import java.time.{ZoneOffset, ZonedDateTime}

object TemporalKey {
  def apply(dateTime: ZonedDateTime): TemporalKey =
    TemporalKey(dateTime.toInstant.toEpochMilli)

  implicit def dateTimeToKey(time: ZonedDateTime): TemporalKey =
    TemporalKey(time)

  implicit def keyToDateTime(key: TemporalKey): ZonedDateTime =
    key.time

  implicit def ordering[A <: TemporalKey]: Ordering[A] =
    Ordering.by(tk => tk.instant)

}

/** A TemporalKey designates the temporal positioning of a layer's tile. */
case class TemporalKey(instant: Long) {
  def time: ZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
}
