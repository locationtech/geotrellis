package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import spray.json.JsonFormat

trait LayerMover[ID, K] {
  def move[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID): Unit
  def move[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, keyIndex: I): Unit
  def move(from: ID, to: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
