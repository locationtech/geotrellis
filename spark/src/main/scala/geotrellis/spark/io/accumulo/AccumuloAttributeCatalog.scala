package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._

import spray.json._

import scala.collection.JavaConversions._

import org.apache.spark.Logging
import org.apache.accumulo.core.client.Connector
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data._
import org.apache.hadoop.io.Text

class AccumuloAttributeCatalog(connector: Connector, val catalogTable: String) extends AttributeCatalog with Logging {
  type ReadableWritable[T] = RootJsonFormat[T]

  private def fetch(layerId: LayerId, attributeName: String): List[Value] = {
    val scanner  = connector.createScanner(catalogTable, new Authorizations())
    scanner.setRange(new Range(new Text(layerId.toString)))
    scanner.fetchColumnFamily(new Text(attributeName))
    scanner.iterator.toList.map(_.getValue)
  }

  def load[T: RootJsonFormat](layerId: LayerId, attributeName: String): T = {
    val values = fetch(layerId, attributeName)

    if(values.size == 0) {
      sys.error(s"Attribute $attributeName not found for layer $layerId")
    } else if(values.size > 1) {
      sys.error(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      values.head.toString.parseJson.convertTo[T]
    }
  }

  def save[T: RootJsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val mutation = new Mutation()
      mutation.put( //RasterMetaData
        new Text(attributeName), new Text(), System.currentTimeMillis(),
        new Value(value.toJson.compactPrint.getBytes)
      )

    connector.write(catalogTable, mutation)
  }
}
