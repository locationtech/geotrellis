package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import spray.json.JsonFormat

trait LayerReindexer[ID, K] {
  def reindex[I <: KeyIndex[K]: JsonFormat](id: ID, keyIndex: I): Unit
  def reindex(id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
