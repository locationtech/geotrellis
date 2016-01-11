package geotrellis.spark.io.accumulo

import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerReindexer {
  def custom[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    keyIndexMethod: KeyIndexMethod[K, TI],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    val layerReaderFrom = new AccumuloLayerReader[K, V, M, FI](attributeStore, new AccumuloRDDReader[K, V](instance))
    val layerReaderTo = new AccumuloLayerReader[K, V, M, TI](attributeStore, new AccumuloRDDReader[K, V](instance))
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerWriter = new AccumuloLayerWriter[K, V, M, TI](
      attributeStore = attributeStore,
      rddWriter      = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = keyIndexMethod,
      table          = table
    )

    val layerCopierFrom = new SparkLayerCopier[AccumuloLayerHeader, K, V, M, TI](
      attributeStore = attributeStore,
      layerReader    = layerReaderFrom,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = header.copy(tileTable = table)
    }

    val layerCopierTo = new SparkLayerCopier[AccumuloLayerHeader, K, V, M, TI](
      attributeStore = attributeStore,
      layerReader    = layerReaderTo,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = header.copy(tileTable = table)
    }

    val layerMover = GenericLayerMover(layerCopierTo, layerDeleter)

    GenericLayerReindexer(layerDeleter, layerCopierFrom, layerMover)
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    keyIndexMethod: KeyIndexMethod[K, KeyIndex[K]],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext): LayerReindexer[LayerId] =
    custom[K, V, M, KeyIndex[K], KeyIndex[K]](instance, table, keyIndexMethod, strategy)
}
