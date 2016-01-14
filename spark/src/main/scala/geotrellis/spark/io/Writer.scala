package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import spray.json.JsonFormat

trait Writer[K, J, V] {
  def write(key: K, value: V, keyIndexMethod: KeyIndexMethod[J]): Unit
  def write[I <: KeyIndex[J]: JsonFormat](key: K, value: V, keyIndex: I): Unit
}
