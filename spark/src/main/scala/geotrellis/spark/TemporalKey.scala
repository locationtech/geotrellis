package geotrellis.spark

import monocle._

object TemporalKey {
  implicit def _temporalComponent = SimpleLens[TemporalKey, TemporalKey](k => k, (_, k) => k)

  implicit def doubleToKey(time: Double): TemporalKey =
    TemporalKey(time)

  implicit def keyToDouble(key: TemporalKey): Double =
    key.time

  implicit def ordering[A <: TemporalKey]: Ordering[A] =
    Ordering.by(tk => tk.time)
}

/** A TemporalKey designates the temporal positioning of a layer's tile. */
case class TemporalKey(time: Double)

trait TemporalComponent[K] extends SimpleLens[K, TemporalKey]
