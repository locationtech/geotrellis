package geotrellis.spark.io.hadoop

import geotrellis.spark.render._
import geotrellis.spark.{LayerId, SpatialKey}

import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD

import scala.collection.concurrent.TrieMap

object SaveToHadoop {
  /**
    * @param id           A Layer ID
    * @param pathTemplate The template used to convert a Layer ID and a SpatialKey into a Hadoop URI
    *
    * @return             A functon which takes a spatial key and returns a Hadoop URI
    */
  def spatialKeyToPath(id: LayerId, pathTemplate: String): (SpatialKey => String) = {
    // Return λ
    { key =>
      pathTemplate
        .replace("{x}", key.col.toString)
        .replace("{y}", key.row.toString)
        .replace("{z}", id.zoom.toString)
        .replace("{name}", id.name)
    }
  }

  /** Sets up saving to Hadoop, but returns an RDD so that writes can be chained.
    *
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def setup[K](rdd: RDD[(K, Array[Byte])])(
    keyToUri: K => String
  ): RDD[(K, Array[Byte])] = {
    rdd.mapPartitions { partition =>
      var fsCache = TrieMap.empty[String, FileSystem]

      for ( row @ (key, data) <- partition ) yield {
        val path = keyToUri(key)
        val uri = new URI(path)
        val fs = fsCache.getOrElseUpdate(
          uri.getScheme,
          FileSystem.get(uri, rdd.context.hadoopConfiguration))
        val out = fs.create(new Path(path))
        try { out.write(data) }
        finally { out.close() }
        row
      }
    }
  }

  /** Saves to Hadoop FileSystem, returns an count of records saved.
    *
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def apply[K](rdd: RDD[(K, Array[Byte])])(
    keyToUri: K => String
  ): Long =
    setup(rdd)(keyToUri).count

}
