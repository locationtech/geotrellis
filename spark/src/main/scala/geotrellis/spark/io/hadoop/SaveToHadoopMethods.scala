package geotrellis.spark.io.hadoop

import geotrellis.spark.render._
import geotrellis.spark.{LayerId, GridKey}

import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD


object SaveToHadoopMethods {
  /**
    * @param id           A Layer ID
    * @param pathTemplate The template used to convert a Layer ID and a GridKey into a Hadoop URI
    *
    * @return             A functon which takes a spatial key and returns a Hadoop URI
    */
  def spatialKeyToPath(id: LayerId, pathTemplate: String): (GridKey => String) = {
    // Return λ
    { key =>
      pathTemplate
        .replace("{x}", key.col.toString)
        .replace("{y}", key.row.toString)
        .replace("{z}", id.zoom.toString)
        .replace("{name}", id.name)
    }
  }
}

class SaveToHadoopMethods[K](rdd: RDD[(K, Array[Byte])]) {

  /**
    * @param keyToPath A function from K (a key) to a Hadoop URI
    */
  def saveToHadoop(keyToPath: K => String): Unit = {
    val scheme = new URI(keyToPath(rdd.first._1)).getScheme

    saveToHadoop(scheme, keyToPath, rdd)
  }

  /**
    * @param scheme    URI scheme, used to get a hadoop FileSystem object
    * @param keyToPath A function from K (a key) to a Hadoop URI
    * @param rdd       An RDD of key, image pairs
    */
  def saveToHadoop(
    scheme: String,
    keyToPath: K => String,
    rdd: RDD[(K, Array[Byte])]
  ): Unit = {
    setupSaveToHadoop(scheme, keyToPath).foreach { x => }
  }

  /** Sets up saving to Hadoop, but returns an RDD so that writes can be chained.
    *
    * @param scheme    URI scheme, used to get a hadoop FileSystem object
    * @param keyToPath A function from K (a key) to a Hadoop URI
    */
  def setupSaveToHadoop(
    scheme: String,
    keyToPath: K => String
  ): RDD[(K, Array[Byte])] = {
    rdd.mapPartitions { partition =>
      val fs = FileSystem.get(new URI(scheme + ":/"), new Configuration)
      for ( (key, data) <- partition ) yield {
        val tilePath = new Path(keyToPath(key))
        val out = fs.create(tilePath)
        try { out.write(data) }
        finally { out.close() }
        (key, data)
      }
    }
  }
}
