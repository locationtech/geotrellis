package geotrellis.spark.io.hadoop

import geotrellis.spark.render._
import geotrellis.spark.{LayerId, SpatialKey}

import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD


class SaveToHadoopMethods[K, V](rdd: RDD[(K, V)]) {
  /** Sets up saving to Hadoop, but returns an RDD so that writes can be chained.
    *
    * @param keyToUri      maps each key to full hadoop supported path
    * @param getBytes  K and V both provided in case K contains required information, like extent.
    */
  def setupSaveToHadoop(keyToUri: K => String)(getBytes: (K, V) => Array[Byte]): RDD[(K, V)] =
    SaveToHadoop.setup(rdd, keyToUri, getBytes)

  /** Saves to Hadoop, but returns a count of records saved.
    *
    * @param keyToUri      maps each key to full hadoop supported path
    * @param getBytes  K and V both provided in case K contains required information, like extent.
    */
  def saveToHadoop(keyToUri: K => String)(getBytes: (K, V) => Array[Byte]): Long =
    SaveToHadoop(rdd, keyToUri, getBytes)
}
