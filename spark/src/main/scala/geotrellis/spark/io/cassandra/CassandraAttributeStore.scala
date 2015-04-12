package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._

import spray.json._

import com.datastax.driver.core.DataType.text
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{set, eq => eqs}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.{ResultSet, Session}

import org.apache.spark.Logging

object CassandraAttributeStore {
  def apply(session: Session, keyspace: String, attributeTable: String): CassandraAttributeStore =
    new CassandraAttributeStore(session, keyspace, attributeTable)
}

class CassandraAttributeStore(session: Session, val keyspace: String, val attributeTable: String) extends AttributeStore with Logging {
  type ReadableWritable[T] = RootJsonFormat[T]

  // Create the attribute table if it doesn't exist
  {
    val schema = SchemaBuilder.createTable(keyspace, attributeTable).ifNotExists()
      .addPartitionKey("layerId", text)
      .addClusteringColumn("name", text)
      .addColumn("value", text)

    session.execute(schema)
  }

  private def fetch(layerId: LayerId, attributeName: String): ResultSet = {
    val query = QueryBuilder.select.column("value")
      .from(keyspace, attributeTable)
      .where (eqs("layerId", layerId.toString))
      .and   (eqs("name", attributeName))

    session.execute(query)
  }

  def read[T: RootJsonFormat](layerId: LayerId, attributeName: String): T = {
    val values = fetch(layerId, attributeName)

    val size = values.getAvailableWithoutFetching
    if(size == 0) {
      sys.error(s"Attribute $attributeName not found for layer $layerId")
    } else if (size > 1) {
      sys.error(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      values.one.getString("value").parseJson.convertTo[T]
    }
  }

  def write[T: RootJsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val update = QueryBuilder.update(keyspace, attributeTable)
      .`with`(set("value", value.toJson.compactPrint))
      .where (eqs("layerId", layerId.toString))
      .and   (eqs("name", attributeName))

    val results = session.execute(update)
  }
}
