package geotrellis.spark

import monocle.SimpleLens

case class SpaceTimeKey(spatialKey: SpatialKey, temporalKey: TemporalKey)

object SpaceTimeKey {
  implicit def _spatialComponent: SpatialComponent[SpaceTimeKey] = 
    SimpleLens[SpaceTimeKey, SpatialKey](k => k.spatialKey, (k, sk) => SpaceTimeKey(sk, k.temporalKey))

  implicit def _temporalComponent: TemporalComponent[SpaceTimeKey] = 
    SimpleLens[SpaceTimeKey, TemporalKey](k => k.temporalKey, (k, tk) => SpaceTimeKey(k.spatialKey, tk))

  implicit def ordering: Ordering[SpaceTimeKey] =
    Ordering.by(stk => (stk.spatialKey, stk.temporalKey))
}
