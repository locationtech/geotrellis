package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.AttributeStore.Fields

import org.apache.spark.SparkContext
import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.reflect.ClassTag

class S3LayerManager(attributeStore: FileAttributeStore)(implicit sc: SparkContext) {
  def delete[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](id: LayerId): Unit = {
    val deleter = FileLayerDeleter[K, V, M](attributeStore)
    deleter.delete(id)
  }

  def copy[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (from: LayerId, to: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[FileLayerHeader](from, Fields.header)
    val copier = FileLayerCopier[K, V, M](attributeStore)
    copier.copy(from, to)
  }

  def move[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (from: LayerId, to: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[FileLayerHeader](from, Fields.header)
    val mover = FileLayerMover[K, V, M](attributeStore)
    mover.move(from, to)
  }

  def reindex[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val reindexer = FileLayerReindexer[K, V, M](attributeStore, keyIndexMethod)
    reindexer.reindex(id)
  }
}
