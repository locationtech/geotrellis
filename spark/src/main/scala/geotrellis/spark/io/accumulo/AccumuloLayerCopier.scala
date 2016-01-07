package geotrellis.spark.io.accumulo

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerCopier {
  def apply[
    K: Boundable: JsonFormat: ClassTag, V: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    instance   : AccumuloInstance,
    layerReader: AccumuloLayerReader[K, V, M, I],
    layerWriter: AccumuloLayerWriter[K, V, M, I]
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M, I] = {
    val writerAttributeStore = layerWriter.attributeStore
    new SparkLayerCopier[AccumuloLayerHeader, K, V, M, I](
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = {
        val writerHeader = writerAttributeStore.readLayerAttribute[AccumuloLayerHeader](id, Fields.header)
        header.copy(tileTable = writerHeader.tileTable)
      }
    }
  }

  def apply[
    K: Boundable: JsonFormat: ClassTag, V: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    attributeStore: AttributeStore[JsonFormat],
    layerReader: AccumuloLayerReader[K, V, M, I],
    layerWriter: AccumuloLayerWriter[K, V, M, I]
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M, I] = {
    val writerAttributeStore = layerWriter.attributeStore
    new SparkLayerCopier[AccumuloLayerHeader, K, V, M, I](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = {
        val writerHeader = writerAttributeStore.readLayerAttribute[AccumuloLayerHeader](id, Fields.header)
        header.copy(tileTable = writerHeader.tileTable)
      }
    }
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    indexMethod: KeyIndexMethod[K, I],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M, I] =
    apply[K, V, M, I](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M, I](instance),
      layerWriter = AccumuloLayerWriter[K, V, M, I](instance, table, indexMethod, strategy)
    )
}
