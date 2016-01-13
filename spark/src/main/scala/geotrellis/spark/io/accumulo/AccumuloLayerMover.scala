package geotrellis.spark.io.accumulo

import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerMover {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    instance: AccumuloInstance,
    layerReader: AccumuloLayerReader[K, V, M],
    layerWriter: AccumuloLayerWriter[K, V, M])
   (implicit sc: SparkContext): LayerMover[LayerId, K] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    new GenericLayerMover[LayerId, K](
      layerCopier = AccumuloLayerCopier[K, V, M](
        attributeStore = attributeStore,
        layerReader    = layerReader,
        layerWriter    = layerWriter
      ),
      layerDeleter = AccumuloLayerDeleter(AccumuloAttributeStore(instance.connector), instance.connector)
    )
  }

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext): LayerMover[LayerId, K] =
    apply[K, V, M](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M](instance),
      layerWriter = AccumuloLayerWriter[K, V, M](instance, table, strategy)
    )
}
