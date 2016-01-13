package geotrellis.spark.io

import geotrellis.spark.Boundable
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import spray.json.JsonFormat

abstract class FilteringLayerReader[ID, K: Boundable, M, ReturnType] extends LayerReader[ID, K, ReturnType] {

  def read[I <: KeyIndex[K]: JsonFormat](id: ID, rasterQuery: RDDQuery[K, M], numPartitions: Int): ReturnType

  def read[I <: KeyIndex[K]: JsonFormat](id: ID, numPartitions: Int): ReturnType =
    read[I](id, new RDDQuery[K, M], numPartitions)

  def read[I <: KeyIndex[K]: JsonFormat](id: ID): ReturnType =
    read[I](id, defaultNumPartitions)

  def read[I <: KeyIndex[K]: JsonFormat](id: ID, rasterQuery: RDDQuery[K, M]): ReturnType =
    read[I](id, rasterQuery, defaultNumPartitions)

  def read(id: ID, numPartitions: Int): ReturnType =
    read[KeyIndex[K]](id, new RDDQuery[K, M], numPartitions)

  def query(layerId: ID): BoundRDDQuery[K, M, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read[KeyIndex[K]](layerId, _))

  def query(layerId: ID, numPartitions: Int): BoundRDDQuery[K, M, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read[KeyIndex[K]](layerId, _, numPartitions))
}
