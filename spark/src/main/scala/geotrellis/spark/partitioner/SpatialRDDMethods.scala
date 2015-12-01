package geotrellis.spark.partitioner

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark.Boundable
import org.apache.spark.Partitioner
import org.apache.spark.rdd.{CoGroupedRDD, ShuffledRDD, RDD}

import scala.reflect.ClassTag

class SpatialRDDMethods[K, V](self: RDD[(K, V)])
    (implicit kt: ClassTag[K]) extends LazyLogging {

  def partitionBy(part: SpacePartitioner[K]) = {
    self.partitioner match {
      case Some(p: SpacePartitioner[_]) if p.regions sameElements part.regions  =>
        self
      case Some(p: SpacePartitioner[_]) =>
        new ReorderedRDD(self, part.numPartitions, i => part.regionIndex(p.regions(i)))
      case _ =>
        new ShuffledRDD(self.filter(part.containsKey), part)
    }
  }

  def spatialLeftJoin[W](right: RDD[(K, W)]) = {
    val rdds = List(self, right)

    val cogroup = rdds.head.partitioner match {
      case Some(part: SpacePartitioner[K]) =>
        logger.debug(s"Left join $self with $right using SpatialCoGroupRDD")
        new SpatialCoGroupRDD[K](rdds, part)
      case _ =>
        logger.debug(s"Left join $self with $right using CoGroupedRDD")
        new CoGroupedRDD[K](rdds, Partitioner.defaultPartitioner(rdds.head, rdds.tail: _*))
    }

    cogroup.flatMapValues { case Array(l, r) =>
        if (l.isEmpty)
          Iterator.empty
        else if (r.isEmpty)
          for (v <- l.iterator) yield (v, None)
        else
          for (v <- l.iterator; w <- r.iterator) yield (v, Some(w))
      }
  }

  def spatialInnerJoin[W](right: RDD[(K, W)]) = {
    val rdds = List(self, right)

    val part = for {
      leftPart <- SpacePartitioner.getPartitioner(self)
      rightPart <- SpacePartitioner.getPartitioner(right)
      intersection <- leftPart intersect rightPart
    } yield intersection

    val cogroup = part match {
      case Some(part: SpacePartitioner[K]) =>
        logger.debug(s"Inner join $self with $right using SpatialCoGroupRDD")
        new SpatialCoGroupRDD[K](rdds, part)
      case _ =>
        logger.debug(s"Inner join $self with $right using CoGroupedRDD")
        new CoGroupedRDD[K](rdds, Partitioner.defaultPartitioner(rdds.head, rdds.tail: _*))
    }

    cogroup.flatMapValues { case Array(l, r) =>
      if (l.isEmpty || r.isEmpty)
        Iterator.empty
      else
        for (v <- l.iterator; w <- r.iterator) yield (v, w)
    }
  }

  def spatialOuterJoin[W](right: RDD[(K, W)]) = {
    val rdds = List(self, right)

    val part = for {
      leftPart <- SpacePartitioner.getPartitioner(self)
      rightPart <- SpacePartitioner.getPartitioner(right)
    } yield leftPart combine rightPart

    val cogroup = part match {
      case Some(part: SpacePartitioner[K]) =>
        logger.debug(s"Outer join $self with $right using SpatialCoGroupRDD")
        new SpatialCoGroupRDD[K](rdds, part)
      case _ =>
        logger.debug(s"Outer join $self with $right using CoGroupedRDD")
        new CoGroupedRDD[K](rdds, Partitioner.defaultPartitioner(rdds.head, rdds.tail: _*))
    }

    cogroup.flatMapValues { case Array(l, r) =>
      if (l.isEmpty || r.isEmpty)
        Iterator.empty
      else if (l.isEmpty)
        for (w <- r.iterator) yield (None, Some(w))
      else if (r.isEmpty)
        for (v <- r.iterator) yield (Some(v), None)
      else
        for (v <- l.iterator; w <- r.iterator) yield (Some(v), Some(w))
    }
  }
}
