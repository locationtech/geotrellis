package geotrellis.spark.partitioner

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

private [partitioner]
class SpatialCoGroupRDD[K](rdds: Seq[RDD[_ <: Product2[K, _]]], part: SpacePartitioner[K]) extends CoGroupedRDD[K](rdds, part) {
  override def getDependencies: Seq[Dependency[_]] = {
    rdds
      .map { rdd: RDD[_ <: Product2[K, _]] =>
      if (rdd.partitioner == Some(part)) {
        logDebug("Adding one-to-one dependency with " + rdd)
        new OneToOneDependency(rdd)
      } else {
        logDebug("Adding shuffle dependency with " + rdd)
        new ShuffleDependency[K, Any, Any](rdd.filter(r => part.containsKey(r._1)), part, None)
      }
    }
  }
}

object SpatialCoGroupRDD {
  def apply[K: ClassTag, V](rdds: Seq[RDD[(K, V)]], part: SpacePartitioner[K]) = {
    val reorderedRdds =
      rdds.map { rdd =>
        rdd.partitioner match {
          case Some(p: SpacePartitioner[K]) =>
            rdd.partitionBy(part)
          case _ =>
            rdd // this is going to end up being a ShuffleDependency
        }
      }

    new SpatialCoGroupRDD(reorderedRdds, part)
  }
}