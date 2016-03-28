package geotrellis.spark.io.hadoop

import geotrellis.spark.render._
import geotrellis.spark.{LayerId, SpatialKey}

import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD


class SaveBytesToHadoopMethods[K](rdd: RDD[(K, Array[Byte])]) {

  /** Saves to Hadoop FileSystem, returns a count of records saved.
    *
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def saveToHadoop(keyToUri: K => String): Long =
    SaveToHadoop(rdd, keyToUri)

  /** Sets up saving to Hadoop, but returns an RDD so that writes can be chained.
    *
    * @param scheme    URI scheme, used to get a hadoop FileSystem object
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def setupSaveToHadoop(keyToUri: K => String): RDD[(K, Array[Byte])] =
    SaveToHadoop.setup(rdd, keyToUri)
}
