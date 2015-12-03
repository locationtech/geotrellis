package geotrellis.spark.partitioner

import org.apache.spark.rdd._

object SpatialCoGroup {
  def apply[K](_rdds: Seq[RDD[_ <: Product2[K, _]]], part: SpacePartitioner[K]) = {
    val reordered = _rdds.map{ rdd => SpatialCoGroup.reorder(rdd, part)}
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