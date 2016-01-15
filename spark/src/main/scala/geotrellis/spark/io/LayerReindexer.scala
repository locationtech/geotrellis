package geotrellis.spark.io

import geotrellis.spark.io.index.KeyIndexMethod

trait LayerReindexer[ID] {
  def reindex(id: ID): Unit
}
