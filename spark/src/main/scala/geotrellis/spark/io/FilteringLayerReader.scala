package geotrellis.spark.io

import geotrellis.spark.Boundable
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import spray.json.JsonFormat

abstract class FilteringLayerReader[ID, K: Boundable, M, ReturnType] extends LayerReader[ID, K, ReturnType] {

  def read[I <: KeyIndex[K]: JsonFormat](id: ID, rasterQuery: RDDQuery[K, M], numPartitions: Int, format: JsonFormat[I]): ReturnType

  def read[I <: KeyIndex[K]: JsonFormat](id: ID, numPartitions: Int, format: JsonFormat[I]): ReturnType =
    read[I](id, new RDDQuery[K, M], numPartitions, format)

  def read[I <: KeyIndex[K]: JsonFormat](id: ID, format: JsonFormat[I]): ReturnType =
    read[I](id, new RDDQuery[K, M], defaultNumPartitions, format)

  def read(id: ID, rasterQuery: RDDQuery[K, M], numPartitions: Int): ReturnType =
    read(id, rasterQuery, numPartitions, implicitly[JsonFormat[KeyIndex[K]]])

  def read(id: ID, rasterQuery: RDDQuery[K, M]): ReturnType =
    read(id, rasterQuery, defaultNumPartitions)

  def query(layerId: ID): BoundRDDQuery[K, M, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _))

  def query(layerId: ID, numPartitions: Int): BoundRDDQuery[K, M, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _, numPartitions))
}
