package geotrellis.spark.io.accumulo

import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

class AccumuloLayerReindexer[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container](
  instance: AccumuloInstance,
  table: String, // table write to
  strategy: AccumuloWriteStrategy,
  keyIndexMethod: KeyIndexMethod[K]) // key index to write
  (implicit sc: SparkContext,
      val cons: ContainerConstructor[K, V, Container],
   containerEv: Container => Container with RDD[(K, V)]) extends LayerReindexer[LayerId] {

  lazy val attributeStore = AccumuloAttributeStore(instance.connector)
  lazy val layerReader    = new AccumuloLayerReader[K, V, Container](attributeStore, new AccumuloRDDReader[K, V](instance))
  lazy val layerDeleter   = AccumuloLayerDeleter(instance)
  lazy val layerMover     = AccumuloLayerMover(attributeStore, layerCopier, layerDeleter)
  lazy val layerWriter = new AccumuloLayerWriter[K, V, Container](
    attributeStore = attributeStore,
    rddWriter      = new AccumuloRDDWriter[K, V](instance, strategy),
    keyIndexMethod = keyIndexMethod,
    table          = table
  )

  lazy val layerCopier = new SparkLayerCopier[AccumuloLayerHeader, K, V, Container](
    attributeStore = attributeStore,
    layerReader    = layerReader,
    layerWriter    = layerWriter
  ) {
    def headerUpdate(id: LayerId, header: AccumuloLayerHeader): AccumuloLayerHeader = header.copy(tileTable = table)
  }

  def getTmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-tmp")
}
