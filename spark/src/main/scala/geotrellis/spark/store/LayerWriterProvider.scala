package geotrellis.spark.store

import geotrellis.layers.LayerId
import geotrellis.layers.AttributeStore

import java.net.URI


trait LayerWriterProvider {
  def canProcess(uri: URI): Boolean

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId]
}
