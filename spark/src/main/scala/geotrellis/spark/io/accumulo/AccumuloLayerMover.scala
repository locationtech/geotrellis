package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

object AccumuloLayerMover {
  def apply[K: AvroRecordCodec: Boundable: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
   instance: AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, M],
   layerWriter: AccumuloLayerWriter)
  (implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    new GenericLayerMover[LayerId](
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
    strategy: AccumuloWriteStrategy = AccumuloWriteStrategy.DEFAULT)
   (implicit sc: SparkContext): LayerMover[LayerId] =
    apply[K, V, M](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M](instance),
      layerWriter = AccumuloLayerWriter(instance, table, strategy)
    )
}
