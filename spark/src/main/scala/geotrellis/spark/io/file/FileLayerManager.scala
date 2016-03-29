package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.reflect.ClassTag

class FileLayerManager(attributeStore: FileAttributeStore)(implicit sc: SparkContext)
    extends LayerManager[LayerId] {
  def delete(id: LayerId): Unit =
    FileLayerDeleter(attributeStore).delete(id)

  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    FileLayerCopier(attributeStore).copy[K, V, M](from, to)

  def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    FileLayerMover(attributeStore).move[K, V, M](from, to)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit =
    FileLayerReindexer(attributeStore).reindex[K, V, M](id, keyIndexMethod)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit =
    FileLayerReindexer(attributeStore).reindex[K, V, M](id, keyIndex)
}
