package geotrellis.spark.partitioner

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import org.apache.spark._

import scala.collection.mutable.ArrayBuffer
import scala.reflect._

/**
 *
 * @param bounds  Bounds of the region we're partitioning
 */
class SpacePartitioner[K: ClassTag](bounds: KeyBounds[K]) extends Partitioner {
  val index: KeyIndex[K] = {
    val cls = implicitly[ClassTag[K]]
    if (cls == classTag[SpatialKey])
      new ZSpatialKeyIndex().asInstanceOf[KeyIndex[K]]
    else
      ??? // TODO this is stop gap, I expect to have some generic K => Long type class based on index
  }

  val r = 1 //bits per partition space

  val regions = partitionBounds(bounds)
  println(s"Indexed $bounds with ${regions.length} partitions")

  def numPartitions = regions.length

  def partitionBounds(b: KeyBounds[K]): Array[Long] = {
    for {
      (start, end) <- index.indexRanges(b)
      p <- (start >> r) to (end >> r)
    } yield p
  }.distinct.toArray

  /** Find index of given prefix/region in this partitioner, -1 if prefix does not exist */
  def regionIndex(region: Long): Option[Int] = {
    // Note: Consider future design where region can overlap several partitions, would change Option -> List
    val i = regions.indexOf(region)
    if (i > -1) Some(i) else None
  }

  /** Inspect partitions in another partitioner and return array of indexes for those partitions that exist here */
  def alignPartitions(other: SpacePartitioner[K]): Array[Int] = {
    val overlap = new ArrayBuffer[Int]
    for {
      region <- other.regions
      idx <- regionIndex(region)
    } overlap += idx

    overlap.toArray
  }

  /** Will return -1 for keys out of bounds */
  def getPartition(key: Any) = {
    val i = index.toIndex(key.asInstanceOf[K])
    regions.indexOf(i >> r)
  }

  def containsKey(key: Any): Boolean =
    getPartition(key) > -1
}
