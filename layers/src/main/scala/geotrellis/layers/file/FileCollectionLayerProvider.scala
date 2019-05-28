package geotrellis.layers.file

import geotrellis.layers._
import geotrellis.layers._

import java.net.URI
import java.io.File


/**
 * Provides [[FileLayerReader]] instance for URI with `file` scheme.
 * The uri represents local path to catalog root.
 *  ex: `file:/tmp/catalog`
 */
class FileCollectionLayerProvider extends AttributeStoreProvider with ValueReaderProvider with CollectionLayerReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "file") true else false
    case null => true // assume that the user is passing in the path to the catalog
  }

  def attributeStore(uri: URI): AttributeStore = {
    val file = new File(uri)
    new FileAttributeStore(file.getCanonicalPath)
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileValueReader(store, catalogPath)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): CollectionLayerReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileCollectionLayerReader(store, catalogPath)
  }
}
