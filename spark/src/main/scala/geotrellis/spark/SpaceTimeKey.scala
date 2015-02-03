package geotrellis.spark

import monocle._

import org.joda.time.DateTime

case class SpaceTimeKey(spatialKey: SpatialKey, temporalKey: TemporalKey)

object SpaceTimeKey {
  implicit object SpatialComponent extends SpatialComponent[SpaceTimeKey] {
    def lens =  createLens(k => k.spatialKey, sk => k => SpaceTimeKey(sk, k.temporalKey))
  }

  implicit object TemporalComponent extends TemporalComponent[SpaceTimeKey] {
    def lens = createLens(k => k.temporalKey, tk => k => SpaceTimeKey(k.spatialKey, tk))
  }

  implicit def ordering: Ordering[SpaceTimeKey] =
    Ordering.by(stk => (stk.spatialKey, stk.temporalKey))

  def apply(col: Int, row: Int, time: DateTime): SpaceTimeKey =
    SpaceTimeKey(SpatialKey(col, row), TemporalKey(time))
}
