package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.json._
import spray.json.JsonFormat

trait LayerMover[ID, K] {
  def move[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, keyIndex: I): Unit
  def move[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, format: JsonFormat[I]): Unit
  def move(from: ID, to: ID): Unit = move(from, to, implicitly[JsonFormat[KeyIndex[K]]])
  def move(from: ID, to: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
