package geotrellis.spark.io

import geotrellis.spark.io.index.KeyIndexMethod

trait LayerReindexer[K, ID] {
  def reindex(id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
