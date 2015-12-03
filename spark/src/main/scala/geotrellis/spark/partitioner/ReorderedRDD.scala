package geotrellis.spark.partitioner

import org.apache.spark._
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd._

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag


case class ReorderedPartition(index: Int, parentPartition: Option[Partition]) extends Partition

class ReorderedDependency[T](rdd: RDD[T], f: Int => Option[Int]) extends NarrowDependency[T](rdd) {
  def getParents(partitionId: Int): List[Int] = f(partitionId).toList
}

class ReorderedSpaceRDD[K, V](rdd: SpaceRDD[K, V], part: SpacePartitioner[K]) extends RDD[(K, V)](rdd.context, Nil) {
  override val partitioner = Some(part)

  override def getDependencies: Seq[Dependency[_]] = {
    List(new ReorderedDependency(rdd, { i => rdd.part.regionIndex(part.regions(i)) }))
  }

  override def getPartitions = {
    for (index <- 0 until part.numPartitions) yield {
      val targetRegion = part.regions(index)
      val sourceRegion = rdd.part.regionIndex(targetRegion)
      new ReorderedPartition(index, sourceRegion map {
        rdd.getPartitions(_)
      })
    }
  }.toArray

  override def compute(split: Partition, context: TaskContext) = {
    split.asInstanceOf[ReorderedPartition].parentPartition match {
      case Some(p) => rdd.iterator(p, context)
      case None => Iterator.empty
    }
  }
}