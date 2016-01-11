package geotrellis.spark.io.accumulo

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerCopier {
  def custom[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
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

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    instance   : AccumuloInstance,
    layerReader: AccumuloLayerReader[K, V, M, KeyIndex[K]],
    layerWriter: AccumuloLayerWriter[K, V, M, KeyIndex[K]]
   )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M, KeyIndex[K]] = {
    val writerAttributeStore = layerWriter.attributeStore
    new SparkLayerCopier[AccumuloLayerHeader, K, V, M, KeyIndex[K]](
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

  def custom[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
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

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    attributeStore: AttributeStore[JsonFormat],
    layerReader: AccumuloLayerReader[K, V, M, KeyIndex[K]],
    layerWriter: AccumuloLayerWriter[K, V, M, KeyIndex[K]]
   )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M, KeyIndex[K]] = {
    val writerAttributeStore = layerWriter.attributeStore
    new SparkLayerCopier[AccumuloLayerHeader, K, V, M, KeyIndex[K]](
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

  def custom[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    indexMethod: KeyIndexMethod[K, I],
    strategy: AccumuloWriteStrategy
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M, I] =
    custom[K, V, M, I](
      instance    = instance,
      layerReader = AccumuloLayerReader.custom[K, V, M, I](instance),
      layerWriter = AccumuloLayerWriter.custom[K, V, M, I](instance, table, indexMethod, strategy)
    )

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    indexMethod: KeyIndexMethod[K, KeyIndex[K]],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M, KeyIndex[K]] =
    apply[K, V, M](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M](instance),
      layerWriter = AccumuloLayerWriter[K, V, M](instance, table, indexMethod, strategy)
    )
}
