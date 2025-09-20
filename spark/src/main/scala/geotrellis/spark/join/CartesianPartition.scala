package geotrellis.spark.join

import org.apache.spark.Partition
import org.apache.spark.internal.Logging
import org.apache.spark.rdd.RDD

import java.io.{IOException, ObjectOutputStream}
import scala.util.control.NonFatal

// https://github.com/apache/spark/blob/686d84453610e463df7df95395ce6ed36a6efacd/core/src/main/scala/org/apache/spark/rdd/CartesianRDD.scala#L29
private[join] class CartesianPartition(
  idx: Int,
  @transient private val rdd1: RDD[_],
  @transient private val rdd2: RDD[_],
  s1Index: Int,
  s2Index: Int
) extends Partition {

  var s1 = rdd1.partitions(s1Index)
  var s2 = rdd2.partitions(s2Index)
  override val index: Int = idx

  @throws(classOf[IOException])
  private def writeObject(oos: ObjectOutputStream): Unit = CartesianPartition.tryOrIOException {
    // Update the reference to parent split at the time of task serialization
    s1 = rdd1.partitions(s1Index)
    s2 = rdd2.partitions(s2Index)
    oos.defaultWriteObject()
  }
}

object CartesianPartition extends Logging {
  /**
   * Execute a block of code that returns a value, re-throwing any non-fatal uncaught
   * exceptions as IOException. This is used when implementing Externalizable and Serializable's
   * read and write methods, since Java's serializer will not report non-IOExceptions properly;
   * see SPARK-4080 for more context.
   */
  // https://github.com/apache/spark/blob/686d84453610e463df7df95395ce6ed36a6efacd/common/utils/src/main/scala/org/apache/spark/util/SparkErrorUtils.scala#L35
  private def tryOrIOException[T](block: => T): T = {
    try {
      block
    } catch {
      case e: IOException =>
        logError("Exception encountered", e)
        throw e
      case NonFatal(e) =>
        logError("Exception encountered", e)
        throw new IOException(e)
    }
  }
}
