package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import spray.json.JsonFormat

trait GenericLayerCopier[ID, K] extends LayerCopier[ID, K] {
  def copy[FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](from: ID, to: ID, format: JsonFormat[FI], keyIndex: TI): Unit
  def copy[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, keyIndex: I): Unit = copy(from ,to, implicitly[JsonFormat[I]], keyIndex)
  def copy(from: ID, to: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
