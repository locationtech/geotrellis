package geotrellis.spark.partition

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.index.zcurve.{Z3, Z2, ZSpatialKeyIndex}

/** Coarse KeyIndex to be used for partitioning of RDDs.
  * Coarseness means that multiple keys will be mapped to a single SFC value.
  * This many to one mapping forms spatially relate key blocks
  */
trait PartitionerIndex[K] extends Serializable {
  def toIndex(key: K): Long
  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)]
}

object PartitionerIndex {

  /**
    * This is a reasonable default value. Operating on 512x512 tiles of Doubles
    * This partitioner will produces partitions of approximately half a gigabyte.
    */
  implicit object SpatialPartitioner extends  PartitionerIndex[SpatialKey] {
    private def toZ(key: SpatialKey): Z2 = Z2(key.col >> 4, key.row >> 4)

    def toIndex(key: SpatialKey): Long = toZ(key).z

    def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(Long, Long)] =
      Z2.zranges(toZ(keyRange._1), toZ(keyRange._2))
  }

  /**
    * This is hoped to be a reasonable default value.
    * The partitioner groups keys in 16x16 blocks spatially and by year temporally.
    */
  implicit object SpaceTimePartitioner extends  PartitionerIndex[SpaceTimeKey] {
    private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col >> 4, key.row >> 4, key.time.getYear)

    def toIndex(key: SpaceTimeKey): Long = toZ(key).z

    def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
      Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
  }
}
