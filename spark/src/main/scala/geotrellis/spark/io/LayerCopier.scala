package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.json._
import spray.json.JsonFormat

trait LayerCopier[ID, K] {
  def copy[FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](from: ID, to: ID, format: JsonFormat[FI], keyIndex: TI): Unit
  def copy[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, keyIndex: I): Unit = copy(from ,to, implicitly[JsonFormat[I]], keyIndex)
  def copy[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, format: JsonFormat[I]): Unit
  def copy(from: ID, to: ID): Unit = copy(from, to, implicitly[JsonFormat[KeyIndex[K]]])
  def copy(from: ID, to: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
