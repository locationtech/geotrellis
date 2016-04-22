package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class CassandraLayerManager(attributeStore: CassandraAttributeStore, instance: CassandraInstance)(implicit sc: SparkContext)
    extends LayerManager[LayerId]{
  def delete(id: LayerId): Unit =
    CassandraLayerDeleter(attributeStore, instance).delete(id)

  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    CassandraLayerCopier(instance).copy[K, V, M](from, to)

  def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    CassandraLayerMover(instance).move[K, V, M](from, to)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit =
    CassandraLayerReindexer(instance).reindex[K, V, M](id, keyIndexMethod)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit =
    CassandraLayerReindexer(instance).reindex[K, V, M](id, keyIndex)
}
