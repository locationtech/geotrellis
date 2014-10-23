package geotrellis.spark

import monocle._

object SpatialKey {
  implicit def _spatialComponent = SimpleLens[SpatialKey, SpatialKey](k => k, (_, k) => k)

  implicit def tupToKey(tup: (Int, Int)): SpatialKey =
    SpatialKey(tup._1, tup._2)

  implicit def keyToTup(key: SpatialKey): (Int, Int) =
    (key.col, key.row)

  implicit def ordering[A <: SpatialKey]: Ordering[A] =
    Ordering.by(sk => sk.tupled)
}

/** A SpatialKey designates the spatial positioning of a layer's tile. */
case class SpatialKey(col: Int, row: Int)

trait SpatialComponent[K] extends SimpleLens[K, SpatialKey]
