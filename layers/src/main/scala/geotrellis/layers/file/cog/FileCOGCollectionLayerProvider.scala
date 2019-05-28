package geotrellis.layers.file.cog

import geotrellis.layers.LayerId
import geotrellis.layers.{AttributeStore, AttributeStoreProvider}
import geotrellis.layers.cog._
import geotrellis.layers.file.FileAttributeStore

import java.net.URI
import java.io.File


class FileCOGCollectionLayerProvider extends AttributeStoreProvider
  with COGValueReaderProvider with COGCollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "file") true else false
    case null => true // assume that the user is passing in the path to the catalog
  }

  def attributeStore(uri: URI): AttributeStore = {
    val file = new File(uri)
    new FileAttributeStore(file.getCanonicalPath)
  }

  def valueReader(uri: URI, store: AttributeStore): COGValueReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileCOGValueReader(store, catalogPath)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): COGCollectionLayerReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileCOGCollectionLayerReader(store, catalogPath)
  }
}
