package geotrellis.spark.partitioner

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve.{Z3, Z2, ZSpatialKeyIndex}
import org.apache.spark._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import scala.reflect._

/**
 * @param bounds  Bounds of the region we're partitioning
 */
case class SpacePartitioner[K: ClassTag](bounds: KeyBounds[K], r: Int = 4)
    (implicit gridKey: GridKey[K], boundable: Boundable[K]) extends Partitioner {

  val index: KeyIndex[K] = SpacePartitioner.gridKeyIndex(gridKey)

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
    // TODO: unused
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

  def combine(other: SpacePartitioner[K]): SpacePartitioner[K] =
    SpacePartitioner(boundable.combine(bounds, other.bounds))

  def intersect(other: SpacePartitioner[K]): Option[SpacePartitioner[K]] =
    for ( bounds <- boundable.intersect(bounds, other.bounds))
      yield SpacePartitioner(bounds)

  override def equals(other: Any): Boolean = other match {
    case part: SpacePartitioner[K] if part.r == r =>
      part.regions sameElements regions
    case _ =>
      false
  }
}

object SpacePartitioner {
  def getPartitioner[K: ClassTag](rdd: RDD[_ <: Product2[K, _]]): Option[SpacePartitioner[K]] = {
    if (rdd.partitioner.nonEmpty && rdd.partitioner.get.isInstanceOf[SpacePartitioner[K]]) {
      rdd.partitioner.asInstanceOf[Option[SpacePartitioner[K]]]
    } else if (rdd.isInstanceOf[Product2[_,_]]) {
      // If our RDD does not have a partitioner it may have been composed with a partitioner that describes it
      //  so it can be used at later point, like now.
      val pair = rdd.asInstanceOf[Product2[_,_]]
      if (pair._1.isInstanceOf[SpacePartitioner[_]])
        Some(pair._1.asInstanceOf[SpacePartitioner[K]])
      else if (pair._2.isInstanceOf[SpacePartitioner[_]])
        Some(pair._2.asInstanceOf[SpacePartitioner[K]])
      else
        None
    } else {
      None
    }
  }

  /**
   * This function maps a GridKey interface to an SFC.
   * Currently we focus on supporting up to 3D space.
   * This function can become a type class argument to SpacePartitioner if extension is desired.
   */
  def gridKeyIndex[K](gridKey: GridKey[K]): KeyIndex[K] = {
    gridKey.dimensions match  {
      case 3 =>
        new KeyIndex[K] {
          private def toZ(key: K): Z3 = {
            val arr = gridKey(key)
            Z3(arr(0), arr(1), arr(3))
          }

          def toIndex(key: K) = toZ(key).z

          def indexRanges(keyRange: (K, K)) =
            Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
        }

      case 2 =>
        new KeyIndex[K] {
          private def toZ(key: K): Z2 = {
            val arr = gridKey(key)
            Z2(arr(0), arr(1))
          }

          def toIndex(key: K) = toZ(key).z

          def indexRanges(keyRange: (K, K)) =
            Z2.zranges(toZ(keyRange._1), toZ(keyRange._2))
        }

      case 1 =>
        new KeyIndex[K] {
          def toIndex(key: K) = gridKey(key)(0)

          def indexRanges(keyRange: (K, K)) =
            List((gridKey(keyRange._1)(0).toLong, gridKey(keyRange._2)(0).toLong))
        }
      case x =>
        sys.error("Grids of $x dimensions are not supported by SpacePartitioner")
    }
  }
}