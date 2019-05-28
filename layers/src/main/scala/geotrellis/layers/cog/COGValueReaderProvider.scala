package geotrellis.layers.cog

import geotrellis.layers.LayerId
import geotrellis.layers.AttributeStore

import java.net.URI


trait COGValueReaderProvider {
  def canProcess(uri: URI): Boolean

  def valueReader(uri: URI, store: AttributeStore): COGValueReader[LayerId]
}
