package geotrellis.spark.io.accumulo

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark.io.{AttributeStore, SparkLayerCopier, ContainerConstructor}
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
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
   instance   : AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, Container[K]],
   layerWriter: AccumuloLayerWriter[K, V, Container[K]])
  (implicit sc: SparkContext,
          cons: ContainerConstructor[K, V, Container[K]],
   containerEv: Container[K] => Container[K] with RDD[(K, V)]): SparkLayerCopier[AccumuloLayerHeader, K, V, Container[K]] = {
    val writerAttributeStore = layerWriter.attributeStore
    new SparkLayerCopier[AccumuloLayerHeader, K, V, Container[K]](
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

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]]
  (attributeStore: AttributeStore[JsonFormat],
   layerReader: AccumuloLayerReader[K, V, Container[K]],
   layerWriter: AccumuloLayerWriter[K, V, Container[K]])
  (implicit sc: SparkContext,
   cons: ContainerConstructor[K, V, Container[K]],
   containerEv: Container[K] => Container[K] with RDD[(K, V)]): SparkLayerCopier[AccumuloLayerHeader, K, V, Container[K]] =
    apply[K, V, Container](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    )

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: MergeView: ClassTag, Container[_]](
   instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit sc: SparkContext,
          cons: ContainerConstructor[K, V, Container[K]],
   containerEv: Container[K] => Container[K] with RDD[(K, V)]): SparkLayerCopier[AccumuloLayerHeader, K, V, Container[K]] =
    apply[K, V, Container](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, Container](instance),
      layerWriter = AccumuloLayerWriter[K, V, Container](instance, table, indexMethod, strategy)
    )
}
