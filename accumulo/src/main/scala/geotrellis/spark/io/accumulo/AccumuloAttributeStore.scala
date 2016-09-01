package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._

import com.typesafe.config.ConfigFactory
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.apache.spark.Logging
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data._
import org.apache.hadoop.io.Text

import scala.collection.JavaConversions._

object AccumuloAttributeStore {
  def apply(connector: Connector, attributeTable: String): AccumuloAttributeStore =
    new AccumuloAttributeStore(connector, attributeTable)

  def apply(connector: Connector): AccumuloAttributeStore =
    apply(connector, ConfigFactory.load().getString("geotrellis.accumulo.catalog"))

  def apply(instance: AccumuloInstance, attributeTable: String): AccumuloAttributeStore =
    apply(instance.connector, attributeTable)

  def apply(instance: AccumuloInstance): AccumuloAttributeStore =
    apply(instance.connector)
}

class AccumuloAttributeStore(val connector: Connector, val attributeTable: String) extends DiscreteLayerAttributeStore with Logging {
  //create the attribute table if it does not exist
  {
    val ops = connector.tableOperations()
    if (!ops.exists(attributeTable))
      ops.create(attributeTable)
  }

  val SEP = "__.__"

  def layerIdText(layerId: LayerId): Text =
    s"${layerId.name}${SEP}${layerId.zoom}"

  private def fetch(layerId: Option[LayerId], attributeName: String): Iterator[Value] = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    layerId.foreach { id =>
      scanner.setRange(new Range(layerIdText(id)))
    }
    scanner.fetchColumnFamily(new Text(attributeName))
    scanner.iterator.map(_.getValue)
  }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit = {
    if(!layerExists(layerId)) throw new LayerNotFoundError(layerId)
    val numThreads = 1
    val config = new BatchWriterConfig()
    config.setMaxWriteThreads(numThreads)
    val deleter = connector.createBatchDeleter(attributeTable, new Authorizations(), numThreads, config)
    deleter.setRanges(List(new Range(layerIdText(layerId))))
    attributeName.foreach { name =>
      deleter.fetchColumnFamily(new Text(name))
    }
    deleter.delete()
    attributeName match {
      case Some(attribute) => clearCache(layerId, attribute)
      case None => clearCache(layerId)
    }
  }

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    val values = fetch(Some(layerId), attributeName).toVector

    if(values.isEmpty) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if(values.size > 1) {
      throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      values.head.toString.parseJson.convertTo[(LayerId, T)]._2
    }
  }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId,T] = {
    fetch(None, attributeName)
      .map { _.toString.parseJson.convertTo[(LayerId, T)] }
      .toMap
  }

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val mutation = new Mutation(layerIdText(layerId))
    mutation.put(
      new Text(attributeName), new Text(), System.currentTimeMillis(),
      new Value((layerId, value).toJson.compactPrint.getBytes)
    )

    connector.write(attributeTable, mutation)
  }

  def layerExists(layerId: LayerId): Boolean = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    scanner.iterator
      .exists { kv =>
        val List(name, zoomStr) = kv.getKey.getRow.toString.split(SEP).toList
        layerId == LayerId(name, zoomStr.toInt)
      }
  }

  def delete(layerId: LayerId): Unit = delete(layerId, None)

  def delete(layerId: LayerId, attributeName: String): Unit = delete(layerId, Some(attributeName))

  def layerIds: Seq[LayerId] = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    scanner.iterator
      .map { kv =>
        val List(name, zoomStr) = kv.getKey.getRow.toString.split(SEP).toList
        LayerId(name, zoomStr.toInt)
      }
      .toList
      .distinct
  }

  def availableAttributes(id: LayerId): Seq[String] = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    scanner.setRange(new Range(layerIdText(id)))
    scanner.iterator.map(_.getKey.getColumnFamily.toString).toVector
  }
}
