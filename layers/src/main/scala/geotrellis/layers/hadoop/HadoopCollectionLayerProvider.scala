package geotrellis.layers.hadoop

import geotrellis.layers._
import geotrellis.layers.hadoop.util.HdfsUtils
import geotrellis.util.UriUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration

import java.net.URI


class HadoopCollectionLayerProvider extends AttributeStoreProvider with ValueReaderProvider with CollectionLayerReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => SCHEMES contains str.toLowerCase
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val path = new Path(HdfsUtils.trim(uri))
    val conf = new Configuration()
    HadoopAttributeStore(path, conf)
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val _uri = HdfsUtils.trim(uri)
    val path = new Path(_uri)
    val params = UriUtils.getParams(_uri)
    val conf = new Configuration()
    val maxOpenFiles = params.getOrElse("maxOpenFiles", "16").toInt
    new HadoopValueReader(store, conf, maxOpenFiles)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore) = {
    val _uri = HdfsUtils.trim(uri)
    val path = new Path(_uri)
    val conf = new Configuration()
    HadoopCollectionLayerReader(path, conf)
  }
}
