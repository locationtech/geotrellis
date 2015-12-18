package geotrellis.spark.io.accumulo

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io.Bridge

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class AccumuloLayerManager(attributeStore: AccumuloAttributeStore, instance: AccumuloInstance)(implicit sc: SparkContext) {
  def delete[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](id: LayerId): Unit = {
    val deleter = AccumuloLayerDeleter(attributeStore, instance)
    deleter.delete(id)
  }

  def copy[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (from: LayerId, to: LayerId, keyIndexMethod: KeyIndexMethod[K])
  (implicit bridge: Bridge[(RDD[(K, V)], M), C]): Unit = {
    val header = attributeStore.readLayerAttribute[AccumuloLayerHeader](from, Fields.header)
    val copier = AccumuloLayerCopier[K, V, M, C](instance, header.tileTable, keyIndexMethod, AccumuloLayerWriter.defaultAccumuloWriteStrategy)
    copier.copy(from, to)
  }

  def move[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (from: LayerId, to: LayerId, keyIndexMethod: KeyIndexMethod[K])
  (implicit bridge: Bridge[(RDD[(K, V)], M), C]): Unit = {
    val header = attributeStore.readLayerAttribute[AccumuloLayerHeader](from, Fields.header)
    val mover = AccumuloLayerMover[K, V, M, C](instance, header.tileTable, keyIndexMethod, AccumuloLayerWriter.defaultAccumuloWriteStrategy)
    mover.move(from, to)
  }

  def reindex[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (id: LayerId, keyIndexMethod: KeyIndexMethod[K])(implicit bridge: Bridge[(RDD[(K, V)], M), C]): Unit = {
    val header = attributeStore.readLayerAttribute[AccumuloLayerHeader](id, Fields.header)
    val reindexer = AccumuloLayerReindexer[K, V, M, C](instance, header.tileTable, keyIndexMethod, AccumuloLayerWriter.defaultAccumuloWriteStrategy)
    reindexer.reindex(id)
  }
}
