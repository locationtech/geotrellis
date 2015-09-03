package geotrellis.spark.io.hadoop

import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD

class SaveToHadoopMethods[K, V](rdd: RDD[(K, V)]) {
  /**
   * @param scheme    URI scheme, used to get a hadoop FileSystem object
   * @param path      maps each key to full hadoop supported path
   * @param getBytes  K and V both provided in case K contains required information, like extent.
   */
  def saveToHadoop(scheme: String, path: K => String, getBytes: (K,V) => Array[Byte]): Unit = {
    rdd.foreachPartition{ partition =>
      val fs = FileSystem.get(new URI(scheme + ":/"), new Configuration)
      for ( (key, tile) <- partition ) {
        val tilePath = new Path(path(key))
        val out = fs.create(tilePath)
        try { out.write(getBytes(key, tile)) }
        finally { out.close() }
      }
    }
  }
}
