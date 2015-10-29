package geotrellis.spark.io

import geotrellis.spark._
import org.apache.spark.rdd.RDD

trait Reader[K, V] extends (K => V) {
  def read(key: K): V
  def apply(key: K): V = read(key)
}

/** Third type param may be not useful (think only two should be enough) */
trait Writer[K, V, U] extends ((K,V) => Unit) {
  def write(key: K, value: V): Unit
  def update(key: K, value: U): Unit
  def apply(key: K, value: V): Unit = write(key, value)
}

trait LayerReader[ID, ReturnType] extends Reader[ID, ReturnType] {
  val defaultNumPartitions: Int

  def read(id: ID, numPartitions: Int): ReturnType

  def read(id: ID): ReturnType =
    read(id, defaultNumPartitions)
}

abstract class FilteringLayerReader[ID, K: Boundable, ReturnType] extends LayerReader[ID, ReturnType] {
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
