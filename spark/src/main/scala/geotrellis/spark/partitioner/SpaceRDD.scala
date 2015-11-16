package geotrellis.spark.partitioner

import geotrellis.spark.KeyBounds
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag


/**
 * SpaceRDD needs to carry around it's partitions, so we may know how to construct a SpatialPartitioner
 * When we need to do a SpatialJoin. At this stage, the underlying RDD is very likely not spatially partitioned.
 */
class SpaceRDD[K: ClassTag, V: ClassTag](val rdd: RDD[(K, V)], val bounds: KeyBounds[K]) extends RDD[(K, V)](rdd) {

  override def compute(split: Partition, context: TaskContext) =
    firstParent[(K, V)].iterator(split, context)

  override def getPartitions: Array[Partition] =
    firstParent[(K, V)].partitions

  def getSpacePartitioner: SpacePartitioner[K] =
    partitioner match {
      case Some(leftPart: SpacePartitioner[K]) => leftPart
      case None => new SpacePartitioner[K](bounds)
    }

}
