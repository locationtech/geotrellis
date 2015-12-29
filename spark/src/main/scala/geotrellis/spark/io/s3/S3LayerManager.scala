package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.AttributeStore.Fields

import org.apache.spark.SparkContext
import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.reflect.ClassTag

class S3LayerManager(attributeStore: S3AttributeStore)(implicit sc: SparkContext) {
  def delete[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](id: LayerId): Unit = {
    val deleter = S3LayerDeleter(attributeStore)
    deleter.delete(id)
  }

  def copy[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (from: LayerId, to: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[S3LayerHeader](from, Fields.header)
    val copier = S3LayerCopier[K, V, M](attributeStore, header.bucket, header.key)
    copier.copy(from, to)
  }

  def move[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (from: LayerId, to: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[S3LayerHeader](from, Fields.header)
    val mover = S3LayerMover[K, V, M](attributeStore, header.bucket, header.key)
    mover.move(from, to)
  }

  def reindex[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
     (id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val reindexer = S3LayerReindexer[K, V, M](attributeStore, keyIndexMethod)
    reindexer.reindex(id) // keyIndexMethod should be part of the LayerReindexer trait
  }
}
