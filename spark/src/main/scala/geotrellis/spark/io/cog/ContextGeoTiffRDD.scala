package geotrellis.spark.io.cog

import geotrellis.spark.Metadata

import org.apache.spark._
import org.apache.spark.rdd._

/** Is there any need in this class? */
object ContextGeoTiffRDD {
  def apply[K, V, M](rdd: RDD[(K, V)], metadata: MetadataAccumulator[M]): RDD[(K, V)] with Metadata[MetadataAccumulator[M]] =
    new ContextGeoTiffRDD(rdd, metadata)

  implicit def tupleToContextRDD[K, V, M](tup: (RDD[(K, V)], MetadataAccumulator[M])): ContextGeoTiffRDD[K, V, M] =
    new ContextGeoTiffRDD(tup._1, tup._2)
}

class ContextGeoTiffRDD[K, V, M](val rdd: RDD[(K, V)], val metadata: MetadataAccumulator[M]) extends RDD[(K, V)](rdd) with Metadata[MetadataAccumulator[M]] {
  override val partitioner = rdd.partitioner

  def compute(split: Partition, context: TaskContext) =
    rdd.iterator(split, context)

  def getPartitions: Array[Partition] =
    rdd.partitions
}
