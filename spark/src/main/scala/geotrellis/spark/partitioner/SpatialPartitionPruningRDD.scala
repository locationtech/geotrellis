//package geotrellis.spark.partitioner
//
//import geotrellis.spark.KeyBounds
//import org.apache.spark.rdd._
//import scala.reflect._
//
//object SpatialPartitionPruningRDD {
//  def create[K: ClassTag, V](prev: RDD[(K, V)], bounds: KeyBounds[K]) = {
//    val Some(part: SpacePartitioner[K]) = prev.partitioner
//    val queryPartitions = part.partitionBounds(bounds)
//    // TODO: address the gaps in partitions?
//    PartitionPruningRDD.create(prev, queryPartitions.contains)
//  }
//}