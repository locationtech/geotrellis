package geotrellis.spark.partitioner

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect._


class SpaceRDD[K, V] private (val prev: RDD[(K, V)], val part: SpacePartitioner[K], val bounds: Option[KeyBounds[K]])
    (implicit kt: ClassTag[K])
  extends RDD[(K, V)](prev) with LazyLogging {

  def this(rdd: RDD[(K, V)], part: SpacePartitioner[K])(implicit kt: ClassTag[K]) =
    this(
      rdd match {
        case rdd: SpaceRDD[_, _] =>
          if (part != rdd.part) {
            // This construction is the same as intersection with new Partitioner
            new ReorderedSpaceRDD(rdd, part).filter{ r => part.containsKey(r._1) }
          } else {
            // Nothing needs done here, pass through base RDD
            rdd.prev
          }
        case rdd: RDD[_] =>
          new ShuffledRDD(rdd.filter { t => part.containsKey(t._1) }, part)
      }
      ,part,
      part.bounds)

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
      SpatialCoGroup[K](rdds, part)
        .flatMapValues { case Array(l, r) =>
        if (l.isEmpty)
          Iterator.empty
        else if (r.isEmpty)
          for (v <- l.iterator) yield (v, None)
        else
          for (v <- l.iterator; w <- r.iterator) yield (v, Some(w))
      }.asInstanceOf[RDD[(K, (V, Option[W]))]]

    new SpaceRDD(rdd, part, part.bounds)
  }

  def join[W](right: SpaceRDD[K, W]): SpaceRDD[K, (V, W)] = {
    val joinPart = part intersect right.part

    val rdd = SpatialCoGroup[K](List(this, right), joinPart)
      .flatMapValues { case Array(l, r) =>
        if (l.isEmpty || r.isEmpty)
          Iterator.empty
        else
          for (v <- l.iterator; w <- r.iterator) yield (v, w)
      }.asInstanceOf[RDD[(K, (V,W))]]

    new SpaceRDD[K, (V, W)](rdd, joinPart, joinPart.bounds)
  }

  def filter(bounds: KeyBounds[K]): SpaceRDD[K, V] = {
    val filterPart = part intersect bounds
    val rdd: RDD[(K, V)] = new ReorderedSpaceRDD(this, filterPart)
    val filtered = rdd.filter{ r => bounds.includes(r._1) }
    new SpaceRDD(filtered, filterPart, filterPart.bounds)
  }
}