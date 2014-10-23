package geotrellis.spark

case class SpaceTimeKey(spacialKey: SpatialKey, temporalKey: TemporalKey)

object SpaceTimeKey {
  implicit def _spatialComponent = SimpleLens[SpaceTimeKey, SpatialKey](k => k.spacialKey, (k, sk) => SpaceTimeKey(sk, k.temporalKey))
  implicit def _temporalComponent = SimpleLens[SpaceTimeKey, TemporalKey](k => k.temporalKey, (k, tk) => SpaceTimeKey(k.spatialKey, tk))
}
