package geotrellis.spark.partitioner

import org.apache.spark._
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd._

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag


case class ReorderedPartition(index: Int, parentPartition: Option[Partition]) extends Partition

class ReorderedRDD[T: ClassTag](rdd: RDD[T], numPartitions: Int, f: Int => Option[Int]) extends RDD[T](rdd.context, Nil) {

  val getPartitions = {
    val partitions = new Array[Partition](numPartitions)

    rdd.partitions.foreach { p =>
      f(p.index) match {
        case Some(targetIndex) =>
          partitions(targetIndex) = ReorderedPartition(targetIndex, Some(p))
        case None =>
          // this partition is not used, silently drop it. Bye!
      }
    }

    for (i <- 0 until numPartitions) {
      if (null == partitions(i))
        partitions(i) = new ReorderedPartition(i, None)
    }

    partitions
  }

  def compute(split: Partition, context: TaskContext) = {
    split.asInstanceOf[ReorderedPartition].parentPartition match {
      case Some(p) => rdd.iterator(p, context)
      case None => Iterator.empty
    }
  }
}
