package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HadoopLayerManager(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext)
    extends LayerManager[LayerId] {
  def delete(id: LayerId): Unit =
    HadoopLayerDeleter(attributeStore).delete(id)

  def copy[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: LayerId, to: LayerId): Unit =
    HadoopLayerCopier(attributeStore).copy[K, V, M](from, to)

  def move[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: LayerId, to: LayerId): Unit =
    HadoopLayerMover(attributeStore).move[K, V, M](from, to)

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit =
    HadoopLayerReindexer(attributeStore).reindex[K, V, M](id, keyIndex)

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit =
    HadoopLayerReindexer(attributeStore).reindex[K, V, M](id, keyIndexMethod)
}
