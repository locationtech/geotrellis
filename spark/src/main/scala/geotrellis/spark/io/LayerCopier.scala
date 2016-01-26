package geotrellis.spark.io

import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import spray.json.JsonFormat

trait LayerCopier[ID, K] {
  def copy[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, format: JsonFormat[I]): Unit
  def copy(from: ID, to: ID): Unit = copy(from, to, implicitly[JsonFormat[KeyIndex[K]]])
}
