package geotrellis.spark.io.accumulo

import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerReindexer {
  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    keyIndexMethod: KeyIndexMethod[K, TI],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    val layerReader = new AccumuloLayerReader[K, V, M, FI](attributeStore, new AccumuloRDDReader[K, V](instance))
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerWriter = new AccumuloLayerWriter[K, V, M, TI](
      attributeStore = attributeStore,
      rddWriter      = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = keyIndexMethod,
      table          = table
    )

    val layerCopier = new SparkLayerCopier[AccumuloLayerHeader, K, V, M, FI](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = header.copy(tileTable = table)
    }

    val layerMover = GenericLayerMover(layerCopier, layerDeleter)

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    keyIndexMethod: KeyIndexMethod[K, I],
    strategy: AccumuloWriteStrategy)
    (implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply[K, V, M, I, I](instance, table, keyIndexMethod, strategy)
}
