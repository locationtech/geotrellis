package geotrellis.spark.store

import geotrellis.layers.LayerId
import geotrellis.layers.AttributeStore

import org.apache.spark.SparkContext

import java.net.URI


trait LayerReaderProvider {
  def canProcess(uri: URI): Boolean

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId]
}
