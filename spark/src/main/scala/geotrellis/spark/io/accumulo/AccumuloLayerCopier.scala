package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerCopier {
  def apply[K: AvroRecordCodec: Boundable: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    attributeStore: AttributeStore[JsonFormat],
    layerReader: AccumuloLayerReader[K, V, M],
    layerWriter: LayerWriter[LayerId]
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M] = {
    val writerAttributeStore = layerWriter.attributeStore
    new SparkLayerCopier[AccumuloLayerHeader, K, V, M](
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

  def apply[K: AvroRecordCodec: Boundable: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
   instance   : AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, M],
   layerWriter: LayerWriter[LayerId]
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M] =
    apply[K, V, M](
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader    = layerReader,
      layerWriter    = layerWriter
    )

  def apply[K: AvroRecordCodec: Boundable: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
   instance: AccumuloInstance,
   table: String,
   strategy: AccumuloWriteStrategy = AccumuloWriteStrategy.DEFAULT
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M] =
    apply[K, V, M](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M](instance),
      layerWriter = AccumuloLayerWriter(instance, table, strategy)
    )
}
