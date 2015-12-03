package geotrellis.spark.partitioner

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect._


class SpaceRDD[K, V](val rdd: RDD[(K, V)], val part: SpacePartitioner[K])(implicit kt: ClassTag[K])
  extends RDD[(K, V)](
    rdd match {
      case rdd: SpaceRDD[_, _] =>
        if (part != rdd.part) {
          new ReorderedSpaceRDD(rdd, part)
        } else {
          rdd
        }
      case rdd: RDD[_] =>
        new ShuffledRDD(rdd.filter{ t => part.containsKey(t._1) }, part)
    }
  ) with LazyLogging {

  def this(rdd: RDD[(K, V)], bounds: KeyBounds[K])(implicit kt: ClassTag[K], gk: GridKey[K]) = {
    this(rdd, SpacePartitioner(bounds))
  }

  override def compute(split: Partition, context: TaskContext) =
    firstParent[(K, V)].iterator(split, context)

  override def getPartitions: Array[Partition] =
    firstParent[(K, V)].partitions

  override val partitioner: Option[Partitioner] = Some(part)

  def leftOuterJoin[W](right: RDD[(K, W)]): SpaceRDD[K, (V, Option[W])] = {
    val rdds = List(this, right)

    val rdd =
      SpatialCoGroupRDD[K](rdds, part)
        .flatMapValues { case Array(l, r) =>
        if (l.isEmpty)
          Iterator.empty
        else if (r.isEmpty)
          for (v <- l.iterator) yield (v, None)
        else
          for (v <- l.iterator; w <- r.iterator) yield (v, Some(w))
      }.asInstanceOf[RDD[(K, (V, Option[W]))]]

    new SpaceRDD(rdd, part)
  }

  def join[W](right: SpaceRDD[K, W]): SpaceRDD[K, (V, W)] = {
    val joinPart = part intersect right.part

    val rdd = SpatialCoGroupRDD[K](List(this, right), joinPart)
      .flatMapValues { case Array(l, r) =>
        if (l.isEmpty || r.isEmpty)
          Iterator.empty
        else
          for (v <- l.iterator; w <- r.iterator) yield (v, w)
      }.asInstanceOf[RDD[(K, (V,W))]]

    new SpaceRDD[K, (V, W)](rdd, joinPart)
  }

  def filter(bounds: KeyBounds[K]): SpaceRDD[K, V] = {
    val filterPart = part intersect bounds
    val rdd: RDD[(K, V)] = new ReorderedSpaceRDD(this, filterPart)
    val filtered = rdd.filter{ r => bounds.includes(r._1) }
    new SpaceRDD(filtered, filterPart)
  }
}

object SpaceRDD {
  def apply[K: Boundable: GridKey: ClassTag, V](rdd: RDD[(K, V)], bounds: KeyBounds[K]): SpaceRDD[K, V] = {
    val part = SpacePartitioner(bounds)
    new SpaceRDD(rdd, part)
  }

  def apply[K: ClassTag, V](rdd: RDD[(K, V)], part: SpacePartitioner[K]): SpaceRDD[K, V] = {
    new SpaceRDD(rdd, part)
  }
}