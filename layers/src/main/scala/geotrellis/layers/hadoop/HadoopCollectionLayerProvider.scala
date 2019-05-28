package geotrellis.layers.hadoop

import geotrellis.layers.LayerId
import geotrellis.layers._
import geotrellis.util.UriUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration

import java.net.URI


class HadoopCollectionLayerProvider extends AttributeStoreProvider with ValueReaderProvider with CollectionLayerReaderProvider {
  val schemes: Array[String] = Array("hdfs", "hdfs+file", "s3n", "s3a", "wasb", "wasbs")

  protected def trim(uri: URI): URI =
    if (uri.getScheme.startsWith("hdfs+"))
      new URI(uri.toString.stripPrefix("hdfs+"))
    else uri

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => schemes contains str.toLowerCase
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val path = new Path(trim(uri))
    val conf = new Configuration()
    HadoopAttributeStore(path, conf)
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val _uri = trim(uri)
    val path = new Path(_uri)
    val params = UriUtils.getParams(_uri)
    val conf = new Configuration()
    val maxOpenFiles = params.getOrElse("maxOpenFiles", "16").toInt
    new HadoopValueReader(store, conf, maxOpenFiles)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore) = {
    val _uri = trim(uri)
    val path = new Path(_uri)
    val conf = new Configuration()
    HadoopCollectionLayerReader(path, conf)
  }
}
