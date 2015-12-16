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
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container[_]](
    instance: AccumuloInstance,
    table: String,
    keyIndexMethod: KeyIndexMethod[K],
    strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext,
          cons: ContainerConstructor[K, V, Container[K]],
   containerEv: Container[K] => Container[K] with RDD[(K, V)]): LayerReindexer[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    val layerReader = new AccumuloLayerReader[K, V, Container[K]](attributeStore, new AccumuloRDDReader[K, V](instance))
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerWriter = new AccumuloLayerWriter[K, V, Container[K]](
      attributeStore = attributeStore,
      rddWriter      = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = keyIndexMethod,
      table          = table
    )

    val layerCopier = new SparkLayerCopier[AccumuloLayerHeader, K, V, Container[K]](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = header.copy(tileTable = table)
    }

    val layerMover = GenericLayerMover(layerCopier, layerDeleter)

    LayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}
