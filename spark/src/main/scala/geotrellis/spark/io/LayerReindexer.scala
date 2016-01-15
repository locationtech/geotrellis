package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.json._
import spray.json.JsonFormat

trait LayerReindexer[ID, K] {
  def reindex[I <: KeyIndex[K]: JsonFormat](id: ID, keyIndex: I): Unit
  def reindex[I <: KeyIndex[K]: JsonFormat](id: ID, format: JsonFormat[I]): Unit
  def reindex(id: ID): Unit = reindex(id, implicitly[JsonFormat[KeyIndex[K]]])
  def reindex(id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
