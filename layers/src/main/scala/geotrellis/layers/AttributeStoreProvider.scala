package geotrellis.layers

import java.net.URI


trait AttributeStoreProvider {
  def canProcess(uri: URI): Boolean

  def attributeStore(uri: URI): AttributeStore
}
