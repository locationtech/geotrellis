package geotrellis.spark.io.accumulo

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._
import scala.reflect.ClassTag

class AccumuloLayerReindexer[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: MergeView: ClassTag, Container]
  (instance: AccumuloInstance, table: String, strategy: AccumuloWriteStrategy)
  (implicit sc: SparkContext,
      val cons: ContainerConstructor[K, V, Container],
   containerEv: Container => Container with RDD[(K, V)]) extends LayerReindexer[K, LayerId] {

  lazy val attributeStore = AccumuloAttributeStore(instance.connector)
  lazy val layerReader    = new AccumuloLayerReader(attributeStore, new AccumuloRDDReader[K, V](instance))
  lazy val layerDeleter   = AccumuloLayerDeleter(instance)

  def reindex(id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val (header, _, _, _, _) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, Unit, Unit, Unit, Unit](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val layerWriter = new AccumuloLayerWriter[K, V, Container](
      attributeStore = attributeStore,
      rddWriter      = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = keyIndexMethod,
      table          = header.tileTable
    )

    val layerCopier = new SparkLayerCopier[AccumuloLayerHeader, K, V, Container](
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader = layerReader,
      layerWriter = layerWriter
    )

    lazy val layerMover = AccumuloLayerMover(attributeStore, layerCopier, layerDeleter)
    val tmpId = id.copy(name = s"${id.name}-tmp")

    layerCopier.copy(id, tmpId) // TODO: to check it is unique
    layerDeleter.delete(id)
    layerMover.move(tmpId, id)
  }
}
