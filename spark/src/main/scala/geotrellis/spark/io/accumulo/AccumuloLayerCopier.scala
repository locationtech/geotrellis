package geotrellis.spark.io.accumulo

import geotrellis.spark.Boundable
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

object AccumuloLayerCopier {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
   instance   : AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, M],
   layerWriter: AccumuloLayerWriter[K, V, M]
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M] = {
    new SparkLayerCopier[AccumuloLayerHeader, K, V, M](
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader    = layerReader,
      layerWriter    = layerWriter
    )
  }

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    attributeStore: AttributeStore[JsonFormat],
    layerReader: AccumuloLayerReader[K, V, M],
    layerWriter: AccumuloLayerWriter[K, V, M]
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M] =
    apply[K, V, M](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    )

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
   instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy
  )(implicit sc: SparkContext): SparkLayerCopier[AccumuloLayerHeader, K, V, M] =
    apply[K, V, M](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M](instance),
      layerWriter = AccumuloLayerWriter[K, V, M](instance, table, indexMethod, strategy)
    )
}
