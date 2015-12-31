package geotrellis.spark.io

import geotrellis.spark.Boundable

abstract class FilteringLayerReader[ID, K: Boundable, M, ReturnType] extends LayerReader[ID, ReturnType] {

  def read(id: ID, rasterQuery: RDDQuery[K, M], numPartitions: Int): ReturnType

  def read(id: ID, rasterQuery: RDDQuery[K, M]): ReturnType =
    read(id, rasterQuery, defaultNumPartitions)

  def read(id: ID, numPartitions: Int): ReturnType =
    read(id, new RDDQuery[K, M], numPartitions)

  def query(layerId: ID): BoundRDDQuery[K, M, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _))

  def query(layerId: ID, numPartitions: Int): BoundRDDQuery[K, M, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _, numPartitions))
}
