package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._

import org.apache.spark.SparkContext
import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.reflect.ClassTag

class S3LayerManager(attributeStore: S3AttributeStore)(implicit sc: SparkContext) extends LayerManager[LayerId] {
  def delete[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](id: LayerId): Unit = ???
  def copy[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](from: LayerId, to: LayerId): Unit = ???
  def move[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](from: LayerId, to: LayerId): Unit = ???

  def reindex[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
     (id: LayerId, keyIndexMethod: KeyIndexMethod[K])(implicit bridge: Bridge[(RDD[(K, V)], M), C]): Unit = {
    val reindexer = S3LayerReindexer[K, V, M, C](attributeStore, keyIndexMethod)
    reindexer.reindex(id) // keyIndexMethod should be part of the LayerReindexer trait
  }

}
