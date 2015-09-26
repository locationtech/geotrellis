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

trait RDDReader[ID, ReturnType] extends Reader[ID, ReturnType] {
  val defaultNumPartitions: Int

  def read(id: ID, numPartitions: Int): ReturnType

  def read(id: ID): ReturnType =
    read(id, defaultNumPartitions)
}

abstract class FilteringRDDReader[ID, K: Boundable, ReturnType] extends RDDReader[ID, ReturnType] {
  type MetaDataType

  def read(id: ID, rasterQuery: RDDQuery[K, MetaDataType], numPartitions: Int): ReturnType

  def read(id: ID, rasterQuery: RDDQuery[K, MetaDataType]): ReturnType =
    read(id, rasterQuery, defaultNumPartitions)

  def read(id: ID, numPartitions: Int): ReturnType =
    read(id, new RDDQuery[K, MetaDataType], numPartitions)

  def query(layerId: ID): BoundRDDQuery[K, MetaDataType, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _))

  def query(layerId: ID, numPartitions: Int): BoundRDDQuery[K, MetaDataType, ReturnType] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _, numPartitions))
}