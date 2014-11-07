package geotrellis.spark

import monocle.SimpleLens
import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._

object TemporalKey {
  implicit def _temporalComponent: TemporalComponent[TemporalKey] = 
    SimpleLens[TemporalKey, TemporalKey](k => k, (_, k) => k)

  implicit def dateTimeToKey(time: DateTime): TemporalKey =
    TemporalKey(time)

  implicit def keyToDateTime(key: TemporalKey): DateTime =
    key.time

  implicit def ordering[A <: TemporalKey]: Ordering[A] =
    Ordering.by(tk => tk.time)
}

/** A TemporalKey designates the temporal positioning of a layer's tile. */
case class TemporalKey(time: DateTime)

