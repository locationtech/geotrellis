package geotrellis.spark

import monocle.Lens
import org.joda.time.DateTime

case class SpaceTimeKey(spatialKey: SpatialKey, temporalKey: TemporalKey)

object SpaceTimeKey {
  implicit def _spatialComponent: SpatialComponent[SpaceTimeKey] =
    KeyLens[SpaceTimeKey, SpatialKey](k => k.spatialKey, sk => k => SpaceTimeKey(sk, k.temporalKey))

  implicit def _temporalComponent: TemporalComponent[SpaceTimeKey] =
    KeyLens[SpaceTimeKey, TemporalKey](k => k.temporalKey, tk => k => SpaceTimeKey(k.spatialKey, tk))

  implicit def ordering: Ordering[SpaceTimeKey] =
    Ordering.by(stk => (stk.spatialKey, stk.temporalKey))

  def apply(col: Int, row: Int, time: DateTime): SpaceTimeKey =
    SpaceTimeKey(SpatialKey(col, row), TemporalKey(time))
}
