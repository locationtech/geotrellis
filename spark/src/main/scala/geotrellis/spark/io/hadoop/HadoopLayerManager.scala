package geotrellis.spark.io.hadoop

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.{Boundable, LayerId}

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HadoopLayerManager(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext) {
  def delete[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](id, Fields.header)
    val deleter = HadoopLayerDeleter(header.path)
    deleter.delete(id)
  }

  def copy[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat
  ](from: LayerId, to: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](from, Fields.header)
    val copier = HadoopLayerCopier[K, V, M, I](header.path)
    copier.copy(from, to)
  }

  def move[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat
  ](from: LayerId, to: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](from, Fields.header)
    val mover = HadoopLayerMover[K, V, M, I](header.path)
    mover.move(from, to)
  }

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K, TI])(implicit hadoopFormat: HadoopFormat[K,V]): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](id, Fields.header)
    val reindexer = HadoopLayerReindexer[K, V, M, FI, TI](header.path, keyIndexMethod)
    reindexer.reindex(id)
  }
}
