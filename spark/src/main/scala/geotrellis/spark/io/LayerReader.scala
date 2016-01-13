package geotrellis.spark.io

import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import spray.json.JsonFormat

trait LayerReader[ID, K, ReturnType] extends Reader[ID, ReturnType] {
  val defaultNumPartitions: Int

  def read(id: ID, numPartitions: Int): ReturnType

  def read[I <: KeyIndex[K]: JsonFormat](id: ID, numPartitions: Int): ReturnType

  def read(id: ID): ReturnType =
    read[KeyIndex[K]](id, defaultNumPartitions)
}
