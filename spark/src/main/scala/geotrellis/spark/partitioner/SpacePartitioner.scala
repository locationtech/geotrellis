package geotrellis.spark.partitioner

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve.{Z3, Z2, ZSpatialKeyIndex}
import org.apache.spark._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import scala.reflect._

case class SpacePartitioner[K: Boundable](bounds: Option[KeyBounds[K]], r: Int)
    (implicit gridKey: GridKey[K]) extends Partitioner {

  private val index: KeyIndex[K] = SpacePartitioner.gridKeyIndex(gridKey)

  private def partitionBounds(b: KeyBounds[K]): Array[Long] = {
    for {
      (start, end) <- index.indexRanges(b)
      p <- (start >> r) to (end >> r)
    } yield p
  }.distinct.toArray

  val regions: Array[Long] = bounds match {
    case Some(b) => partitionBounds(b)
    case None => Array.empty
  }

  def numPartitions = regions.length

  def getPartition(key: Any): Int = {
    val i = index.toIndex(key.asInstanceOf[K])
    val region  = i >> r
    val regionIndex = regions.indexOf(i >> r)
    if (regionIndex > -1) {
      regionIndex
    } else {
      // overflow for keys, at this point this should no longer be considered spatially partitioned
      (region % numPartitions).toInt
    }
  }

  def containsKey(key: Any): Boolean = {
    val i = index.toIndex(key.asInstanceOf[K])
    regions.indexOf(i >> r) > -1
  }

  def combine(other: SpacePartitioner[K]): SpacePartitioner[K] =
    other.bounds match {
      case Some(b) =>
        SpacePartitioner(bounds map { _ combine b }, r)
      case None =>
        this
    }

  def intersect(other: KeyBounds[K]): SpacePartitioner[K] =
    SpacePartitioner(bounds flatMap { _ intersect other }, r)

  def intersect(other: SpacePartitioner[K]): SpacePartitioner[K] =
    other.bounds match {
      case Some(b) =>
        SpacePartitioner(bounds flatMap { _ intersect b }, r)
      case None =>
        other
    }

  def regionIndex(region: Long): Option[Int] = {
    // Note: Consider future design where region can overlap several partitions, would change Option -> List
    val i = regions.indexOf(region)
    if (i > -1) Some(i) else None
  }
}

object SpacePartitioner {
  def apply[K: Boundable: GridKey](): SpacePartitioner[K] =
    SpacePartitioner(None, 8)

  def apply[K: Boundable: GridKey](bounds: KeyBounds[K], r: Int): SpacePartitioner[K] =
    SpacePartitioner(Some(bounds), r)

  def apply[K: Boundable: GridKey](bounds: KeyBounds[K]): SpacePartitioner[K] =
    SpacePartitioner(Some(bounds), 8)

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
            val arr: Array[Int] = gridKey(key)
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