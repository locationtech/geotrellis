package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class FileLayerManager(attributeStore: FileAttributeStore)(implicit sc: SparkContext) {
  def delete(id: LayerId): Unit = {
    val deleter = FileLayerDeleter(attributeStore)
    deleter.delete(id)
  }

  def copy[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (from: LayerId, to: LayerId): Unit = {
    val copier = FileLayerCopier[K, V, M](attributeStore)
    copier.copy(from, to)
  }

  def move[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (from: LayerId, to: LayerId): Unit = {
    val mover = FileLayerMover[K, V, M](attributeStore)
    mover.move(from, to)
  }

  def reindex[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (id: LayerId, keyIndexMethod: KeyIndexMethod[K, KeyIndex[K]]): Unit = {
    val reindexer = FileLayerReindexer[K, V, M](attributeStore, keyIndexMethod)
    reindexer.reindex(id)
  }
}
