package geotrellis.spark.partitioner

import geotrellis.spark.{SpatialKey, SpaceTimeKey}

object GridKey {
  implicit object GridSpatialKey extends GridKey[SpatialKey] {
    def dimensions = 2
    def apply(k: SpatialKey) = Array(k.col, k.row)
  }

  implicit object GridSpaceTime extends GridKey[SpaceTimeKey] {
    def dimensions = 3
    def apply(k: SpaceTimeKey) = Array(k.col, k.row, k.time.getDayOfYear * k.time.getYear)
  }
}

trait GridKey[K] extends Serializable {
  def dimensions: Int
  def apply(k: K): Array[Int]
}