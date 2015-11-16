package geotrellis.spark.partitioner

import java.io.{ObjectOutputStream, IOException}

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.util.Utils

class SpatialJoinRDD[K, V] (
  sc: SparkContext,
  rdds: Seq[RDD[(K, V)]],
  spacePartitioner: SpacePartitioner[K]
) extends RDD[(K, V)](sc, rdds.map { new SpatialJoinDependency(_, spacePartitioner) } ) {

  override val partitioner = Some(spacePartitioner)

  protected def getPartitions = {
    // can I pull dependencies there?
    // each dependency may contribute a list of partitions
    {for (idx <- 0 until spacePartitioner.numPartitions) yield {
      new SpatialJoinRDDPartition(idx,
        {
          val region = spacePartitioner.regions(idx)
          for {
            parent <- rdds
            Some(parentPart: SpacePartitioner[K]) = parent.partitioner
          } yield {
            parentPart.regionIndex(region).map(parent.partitions(_)).toList
          }
        }.toArray
      )
    }}.toArray
  }


  def compute(split: Partition, context: TaskContext) = {
    val parentPartitions = split.asInstanceOf[SpatialJoinRDDPartition].parents
    rdds.zip(parentPartitions).iterator.flatMap {
      case (rdd, p) => p.flatMap( idx => rdd.iterator(idx, context))
    }
  }

  // Get the location where most of the partitions of parent RDDs are located
  //override def getPreferredLocations(s: Partition): Seq[String] =
}

class SpatialJoinRDDPartition(
  val idx: Int,
  val parents: Array[List[Partition]]
) extends Partition {
  override val index = idx
  override def hashCode(): Int = idx
}

class SpatialJoinDependency[K, V](
  rdd: RDD[(K, V)],
  childPartitioner: SpacePartitioner[K])
  extends NarrowDependency[(K, V)](rdd) {
  val Some(parentPartitioner: SpacePartitioner[K]) = rdd.partitioner

  override def getParents(partitionId: Int) = {
    val prefix = childPartitioner.regions(partitionId)
    parentPartitioner.regionIndex(prefix).toList
  }
}