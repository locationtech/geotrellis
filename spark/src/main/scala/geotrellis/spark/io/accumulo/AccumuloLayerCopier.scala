package geotrellis.spark.io.accumulo

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark.Boundable
import geotrellis.spark.io.{LayerCopier, ContainerConstructor}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerCopier {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
  V: AvroRecordCodec: MergeView: ClassTag, Container[_]]
  (instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit sc: SparkContext,
          cons: ContainerConstructor[K, V, Container[K]],
   containerEv: Container[K] => Container[K] with RDD[(K, V)]): LayerCopier[AccumuloLayerHeader, K, V, Container[K]] =
    new LayerCopier[AccumuloLayerHeader, K, V, Container[K]](
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader = AccumuloLayerReader[K, V, Container](instance),
      layerWriter = AccumuloLayerWriter[K, V, Container](instance, table, indexMethod, strategy)
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]]
  (instance: AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, Container[K]],
   layerWriter: AccumuloLayerWriter[K, V, Container[K]])
  (implicit sc: SparkContext,
          cons: ContainerConstructor[K, V, Container[K]],
   containerEv: Container[K] => Container[K] with RDD[(K, V)]): LayerCopier[AccumuloLayerHeader, K, V, Container[K]] =
    new LayerCopier[AccumuloLayerHeader, K, V, Container[K]](
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader = layerReader,
      layerWriter = layerWriter
    )

}
