package geotrellis.spark.io.accumulo

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.security.Authorizations
import spray.json.JsonFormat
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConversions._

class AccumuloLayerDeleter(val attributeStore: AttributeStore[JsonFormat], connector: Connector) extends LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val (header, _, _, _, _) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, Unit, Unit, Unit, Unit](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    val numThreads = 1
    val config = new BatchWriterConfig()
    config.setMaxWriteThreads(numThreads)
    val deleter = connector.createBatchDeleter(header.tileTable, new Authorizations(), numThreads, config)
    deleter.fetchColumnFamily(columnFamily(id))
    deleter.setRanges(new AccumuloRange() :: Nil)
    deleter.delete()

    attributeStore.delete(id)
    attributeStore.clearCache()
  }
}

object AccumuloLayerDeleter {
  def apply(attributeStore: AttributeStore[JsonFormat], connector: Connector): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, connector)

  def apply(attributeStore: AttributeStore[JsonFormat], instance: AccumuloInstance): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, instance.connector)

  def apply(instance: AccumuloInstance): AccumuloLayerDeleter =
    apply(AccumuloAttributeStore(instance.connector), instance.connector)
}
