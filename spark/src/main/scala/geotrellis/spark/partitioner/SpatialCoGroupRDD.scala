package geotrellis.spark.partitioner

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

//class SpatialCoGroupRDD[K: ClassTag](_rdds: Seq[RDD[_ <: Product2[K, _]]], part: SpacePartitioner[K])
//  extends CoGroupedRDD[K](_rdds.map{ rdd => SpatialCoGroupRDD.reorder(rdd, part)}, part) {
//  override def getDependencies: Seq[Dependency[_]] = {
//    _rdds
//      .map { rdd: RDD[_ <: Product2[K, _]] =>
//      if (rdd.partitioner == Some(part)) {
//        logInfo("Adding one-to-one dependency with " + rdd)
//        new OneToOneDependency(rdd)
//      } else {
//        logInfo("Adding shuffle dependency with " + rdd)
//        new ShuffleDependency[K, Any, Any](rdd.filter(r => part.containsKey(r._1)), part, None)
//      }
//    }
//  }
//}

object SpatialCoGroupRDD {
  def apply[K](_rdds: Seq[RDD[_ <: Product2[K, _]]], part: SpacePartitioner[K]) = {
    val reordered = _rdds.map{ rdd => SpatialCoGroupRDD.reorder(rdd, part)}
    new CoGroupedRDD[K](reordered, part)
  }

  def reorder[K](rdd: RDD[_ <: Product2[K, _]], part: SpacePartitioner[K]): RDD[_ <: Product2[K, _]] = {
    rdd match {
      case rdd: SpaceRDD[K, Any] @unchecked =>
        if (part != rdd.part)
          new ReorderedSpaceRDD[K, Any](rdd, part)
        else
          rdd.asInstanceOf[RDD[Product2[K, _]]]
      case rdd: RDD[Product2[K,_] @unchecked] =>
        new ShuffledRDD(rdd.filter{ t => part.containsKey(t._1) }, part)
    }
  }.asInstanceOf[RDD[Product2[K, _]]]
}