package geotrellis.spark.io.accumulo

import com.typesafe.config.ConfigFactory
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._
import DefaultJsonProtocol._

import scala.collection.JavaConversions._

import org.apache.spark.Logging
import org.apache.accumulo.core.client.Connector
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data._
import org.apache.hadoop.io.Text

object AccumuloAttributeStore { 
  def apply(connector: Connector, attributeTable: String): AccumuloAttributeStore =
    new AccumuloAttributeStore(connector, attributeTable)

  def apply(connector: Connector): AccumuloAttributeStore =
    apply(connector, ConfigFactory.load().getString("geotrellis.accumulo.catalog"))
}

class AccumuloAttributeStore(connector: Connector, val attributeTable: String) extends AttributeStore[JsonFormat] with Logging {
  //create the attribute table if it does not exist
  {
    val ops = connector.tableOperations()
    if (!ops.exists(attributeTable))
      ops.create(attributeTable)
  }

  private def fetch(layerId: Option[LayerId], attributeName: String): Iterator[Value] = {
    val scanner  = connector.createScanner(attributeTable, new Authorizations())
    layerId.foreach { id =>
      scanner.setRange(new Range(new Text(id.toString)))
    }    
    scanner.fetchColumnFamily(new Text(attributeName))
    scanner.iterator.map(_.getValue)
  }

  def read[T: Format](layerId: LayerId, attributeName: String): T = {
    val values = fetch(Some(layerId), attributeName).toVector

    if(values.size == 0) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if(values.size > 1) {
      throw new CatalogError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      values.head.toString.parseJson.convertTo[(LayerId, T)]._2
    }
  }

  def readAll[T: Format](attributeName: String): Map[LayerId,T] = {
    fetch(None, attributeName)
      .map{ _.toString.parseJson.convertTo[(LayerId, T)] }
      .toMap
  }

  def write[T: Format](layerId: LayerId, attributeName: String, value: T): Unit = {
    val mutation = new Mutation(layerId.toString)
    mutation.put(
      new Text(attributeName), new Text(), System.currentTimeMillis(),
      new Value((layerId, value).toJson.compactPrint.getBytes)
    )

    connector.write(attributeTable, mutation)
  }

  def layerExists(layerId: LayerId): Boolean = {
    fetch(Some(layerId), AttributeStore.Fields.metaData).nonEmpty
  }
}
