package geotrellis.spark.io.hadoop

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io.Bridge

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HadoopLayerManager(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext) {
  def delete[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](id: LayerId): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](id, Fields.header)
    val deleter = HadoopLayerDeleter(header.path)
    deleter.delete(id)
  }

  def copy[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (from: LayerId, to: LayerId)(implicit bridge: Bridge[(RDD[(K, V)], M), C]): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](from, Fields.header)
    val copier = HadoopLayerCopier[K, V, M, C](header.path)
    copier.copy(from, to)
  }

  def move[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (from: LayerId, to: LayerId)(implicit bridge: Bridge[(RDD[(K, V)], M), C]): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](from, Fields.header)
    val mover = HadoopLayerMover[K, V, M, C](header.path)
    mover.move(from, to)
  }

  def reindex[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (id: LayerId, keyIndexMethod: KeyIndexMethod[K])(implicit bridge: Bridge[(RDD[(K, V)], M), C], hadoopFormat: HadoopFormat[K,V]): Unit = {
    val header = attributeStore.readLayerAttribute[HadoopLayerHeader](id, Fields.header)
    val reindexer = HadoopLayerReindexer[K, V, M, C](header.path, keyIndexMethod)
    reindexer.reindex(id)
  }
}
