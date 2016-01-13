package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import spray.json.JsonFormat

trait LayerCopier[ID, K] {
  def copy[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID): Unit
  def copy[FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](from: ID, to: ID, keyIndex: TI): Unit
  def copy(from: ID, to: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
