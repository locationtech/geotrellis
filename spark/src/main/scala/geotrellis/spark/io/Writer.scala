package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import spray.json.JsonFormat
import geotrellis.spark.KeyBounds

trait Writer[K, J, V] extends ((K, V, KeyIndexMethod[J]) => Unit) {
  def write(key: K, value: V, keyIndexMethod: KeyIndexMethod[J]): Unit
  def write[I <: KeyIndex[J]: JsonFormat](key: K, value: V, keyIndex: I, kb: Option[KeyBounds[J]]): Unit
  def apply(key: K, value: V, keyIndexMethod: KeyIndexMethod[J]): Unit = write(key, value, keyIndexMethod)
}
