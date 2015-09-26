package geotrellis.spark.io

import geotrellis.spark._

trait Reader[K, V] extends (K => V){
  def read(key: K): V
  def apply(key: K): V = read(key)
}

trait Writer[K, V] extends ((K,V) => Unit) {
  def write(key: K, value: V): Unit
  def apply(key: K, value: V): Unit = write(key, value)
}

trait SimpleRasterRDDReader[K, ReturnType] extends Reader[K, ReturnType] {
  val defaultNumPartitions: Int

  def read(id: LayerId, numPartitions: Int): ReturnType

  def read(id: LayerId): ReturnType =
    read(id, defaultNumPartitions)
}

abstract class FilteringRasterRDDReader[K: Boundable, ReturnType] extends SimpleRasterRDDReader[K, ReturnType] {
  type MetaDataType

  def read(id: LayerId, rasterQuery: RDDQuery[K, MetaDataType], numPartitions: Int): ReturnType

  def read(id: LayerId, rasterQuery: RDDQuery[K, MetaDataType]): ReturnType =
    read(id, rasterQuery, defaultNumPartitions)

  def read(id: LayerId, numPartitions: Int): ReturnType =
    read(id, new RDDQuery[K, MetaDataType], numPartitions)

  def query(layerId: LayerId): BoundRDDQuery[K, MetaDataType, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _))

  def query(layerId: LayerId, numPartitions: Int): BoundRDDQuery[K, MetaDataType, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _, numPartitions))
}