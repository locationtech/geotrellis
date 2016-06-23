package geotrellis.spark.partition

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve.{Z3, Z2, ZSpatialKeyIndex}
import geotrellis.util._

import org.apache.spark._
import org.apache.spark.rdd.{ShuffledRDD, RDD}

import scala.collection.mutable.ArrayBuffer
import scala.reflect._

/**
  * Uses KeyIndex to partition an RDD in memory, giving its records some spatial locality.
  * When persisting an RDD partitioned by this partitioner we can safely assume that all records
  * contributing to the same SFC index will reside in one partition.
  */
class IndexPartitioner[K](index: KeyIndex[K], count: Int) extends Partitioner {
  val breaks: Array[Long] =
    if (count > 1)
      KeyIndex.breaks(index.keyBounds, index, count - 1).sorted.toArray
    else
      Array(Long.MaxValue)

  def numPartitions = breaks.length + 1

  /**
    * Because breaks define divisions rather than breaks all indexable keys will have a bucket.
    * Keys that are far out of bounds will be assigned to either first or last partition.
    */
  def getPartition(key: Any): Int = {
    val i = index.toIndex(key.asInstanceOf[K])
    val res = java.util.Arrays.binarySearch(breaks, i)
    if (res >= 0) res // matched break exactly, partitions are inclusive on right
    else -(res+1)     // got an insertion point, convert it to corresponding partition
  }
}

object IndexPartitioner {
  def apply[K](index: KeyIndex[K], count: Int): IndexPartitioner[K] =
    new IndexPartitioner(index, count)

  def apply[K](bounds: Bounds[K], indexMethod: KeyIndexMethod[K], count: Int): IndexPartitioner[K] =
    new IndexPartitioner(indexMethod.createIndex(bounds.get), count)
}
