package geotrellis.spark.io.accumulo

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerCopier {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
   instance   : AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, M, C],
   layerWriter: AccumuloLayerWriter[K, V, M, C])
  (implicit sc: SparkContext,
    bridge: Bridge[(RDD[(K, V)], M), C]): SparkLayerCopier[AccumuloLayerHeader, K, V, M, C] = {
    val writerAttributeStore = layerWriter.attributeStore
    new SparkLayerCopier[AccumuloLayerHeader, K, V, M, C](
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

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (attributeStore: AttributeStore[JsonFormat],
   layerReader: AccumuloLayerReader[K, V, M, C],
   layerWriter: AccumuloLayerWriter[K, V, M, C])
  (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): SparkLayerCopier[AccumuloLayerHeader, K, V, M, C] =
    apply[K, V, M, C](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    )

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: MergeView: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
   instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
  (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): SparkLayerCopier[AccumuloLayerHeader, K, V, M, C] =
    apply[K, V, M, C](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M, C](instance),
      layerWriter = AccumuloLayerWriter[K, V, M, C](instance, table, indexMethod, strategy)
    )
}
