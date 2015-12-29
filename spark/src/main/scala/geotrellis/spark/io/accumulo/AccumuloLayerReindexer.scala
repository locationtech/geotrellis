package geotrellis.spark.io.accumulo

import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerReindexer {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    keyIndexMethod: KeyIndexMethod[K],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    val layerReader = new AccumuloLayerReader[K, V, M](attributeStore, new AccumuloRDDReader[K, V](instance))
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerWriter = new AccumuloLayerWriter[K, V, M](
      attributeStore = attributeStore,
      rddWriter      = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = keyIndexMethod,
      table          = table
    )

    val layerCopier = new SparkLayerCopier[AccumuloLayerHeader, K, V, M](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = header.copy(tileTable = table)
    }

    val layerMover = GenericLayerMover(layerCopier, layerDeleter)

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}
