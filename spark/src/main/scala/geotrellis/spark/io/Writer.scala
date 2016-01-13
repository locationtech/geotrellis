package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import spray.json.JsonFormat

trait Writer[ID, V, K] {
  def write(key: ID, value: V, keyIndexMethod: KeyIndexMethod[K]): Unit
  def write[I <: KeyIndex[K]: JsonFormat](key: ID, value: V, keyIndex: I): Unit
}
