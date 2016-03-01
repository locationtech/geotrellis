package geotrellis.spark.io.accumulo

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._
import geotrellis.spark.{Boundable, LayerId}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class AccumuloLayerManager(attributeStore: AccumuloAttributeStore, instance: AccumuloInstance)(implicit sc: SparkContext) {
  def delete(id: LayerId): Unit =
    AccumuloLayerDeleter(attributeStore, instance).delete(id)

  def copy[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: LayerId, to: LayerId): Unit =
    AccumuloLayerCopier(instance).copy[K, V, M](from, to)

  def move[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: LayerId, to: LayerId): Unit =
    AccumuloLayerMover(instance).move[K, V, M](from, to)

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit =
    AccumuloLayerReindexer(instance).reindex[K, V, M](id, keyIndexMethod)

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit =
    AccumuloLayerReindexer(instance).reindex[K, V, M](id, keyIndex)
}
