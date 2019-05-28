package geotrellis.layers

import java.net.URI


trait ValueReaderProvider {
  def canProcess(uri: URI): Boolean

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId]
}
