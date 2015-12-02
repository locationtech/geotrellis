package geotrellis.spark.partitioner

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect._


class SpaceRDD[K: ClassTag, V] private (val rdd: RDD[(K, V)], val part: SpacePartitioner[K])
  extends RDD[(K, V)](new ShuffledRDD(rdd.filter{ t => part.containsKey(t._1) }, part)) with LazyLogging {

  def bounds = part.bounds

  override def compute(split: Partition, context: TaskContext) =
    firstParent[(K, V)].iterator(split, context)

  override def getPartitions: Array[Partition] =
    firstParent[(K, V)].partitions

  def leftOuterJoin[W](right: RDD[(K, W)]): SpaceRDD[K, (V, Option[W])] = {
    val rdds = List(this, right)

    val rdd =
      new SpatialCoGroupRDD[K](rdds, part)
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
    part intersect right.part match {
      case Some(joinPart) =>
        val rdd = new SpatialCoGroupRDD[K](List(this, right), joinPart)
          .flatMapValues { case Array(l, r) =>
            if (l.isEmpty || r.isEmpty)
              Iterator.empty
            else
              for (v <- l.iterator; w <- r.iterator) yield (v, w)
          }.asInstanceOf[RDD[(K, (V,W))]]

        new SpaceRDD[K, (V, W)](rdd, joinPart)

      case None =>
        ???
        //sparkContext.emptyRDD
    }
  }
}

object SpaceRDD {
  def apply[K: Boundable: GridKey: ClassTag, V](rdd: RDD[(K, V)], bounds: KeyBounds[K]): SpaceRDD[K, V] = {
    val part = new SpacePartitioner(bounds)
    new SpaceRDD(rdd, part)
  }
}