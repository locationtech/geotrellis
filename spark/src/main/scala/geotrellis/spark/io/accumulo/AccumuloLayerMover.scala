package geotrellis.spark.io.accumulo

import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerMover {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    instance: AccumuloInstance,
    layerReader: AccumuloLayerReader[K, V, M, I],
    layerWriter: AccumuloLayerWriter[K, V, M, I])
  (implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    new GenericLayerMover[LayerId](
      layerCopier = AccumuloLayerCopier[K, V, M, I](
        attributeStore = attributeStore,
        layerReader    = layerReader,
        layerWriter    = layerWriter
      ),
      layerDeleter = AccumuloLayerDeleter(AccumuloAttributeStore(instance.connector), instance.connector)
    )
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    indexMethod: KeyIndexMethod[K, I],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext): LayerMover[LayerId] =
    apply[K, V, M, I](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M, I](instance),
      layerWriter = AccumuloLayerWriter[K, V, M, I](instance, table, indexMethod, strategy)
    )
}
