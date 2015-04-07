package geotrellis.spark

import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._

object TemporalKey {
  implicit object TemporalComponent extends IdentityComponent[TemporalKey]

  implicit def dateTimeToKey(time: DateTime): TemporalKey =
    TemporalKey(time)

  implicit def keyToDateTime(key: TemporalKey): DateTime =
    key.time

  implicit def ordering[A <: TemporalKey]: Ordering[A] =
    Ordering.by(tk => tk.time)
}

/** A TemporalKey designates the temporal positioning of a layer's tile. */
case class TemporalKey(time: DateTime)
