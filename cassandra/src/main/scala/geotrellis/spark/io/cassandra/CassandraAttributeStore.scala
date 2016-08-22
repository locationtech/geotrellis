package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._

import com.datastax.driver.core.{ResultSet, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{set, eq => eqs}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.DataType._
import com.typesafe.config.ConfigFactory
import org.apache.spark.Logging
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConversions._

object CassandraAttributeStore {
  def apply(instance: CassandraInstance, attributeKeyspace: String, attributeTable: String): CassandraAttributeStore =
    new CassandraAttributeStore(instance, attributeKeyspace, attributeTable)

  def apply(instance: CassandraInstance): CassandraAttributeStore =
    apply(instance, Cassandra.cfg.getString("keyspace"), Cassandra.cfg.getString("catalog"))
}

class CassandraAttributeStore(val instance: CassandraInstance, val attributeKeyspace: String, val attributeTable: String) extends DiscreteLayerAttributeStore with Logging {

  //create the attribute table if it does not exist
  instance.withSessionDo { session =>
    instance.ensureKeyspaceExists(attributeKeyspace, session)
    session.execute(
      SchemaBuilder.createTable(attributeKeyspace, attributeTable).ifNotExists()
        .addPartitionKey("layerId", text)
        .addClusteringColumn("name", text)
        .addColumn("value", text)
    )
  }

  val SEP = "__.__"

  def layerIdString(layerId: LayerId): String =
    s"${layerId.name}${SEP}${layerId.zoom}"

  private def fetch(layerId: Option[LayerId], attributeName: String): ResultSet = instance.withSessionDo { session =>
    val query =
      layerId match {
        case Some(id) =>
          QueryBuilder.select.column("value")
            .from(attributeKeyspace, attributeTable)
            .where(eqs("layerId", layerIdString(id)))
            .and(eqs("name", attributeName))
        case None =>
          QueryBuilder.select.column("value")
            .from(attributeKeyspace, attributeTable)
            .where(eqs("name", attributeName))
      }

    session.execute(query)
  }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit = instance.withSessionDo { session =>
    if (!layerExists(layerId)) throw new LayerNotFoundError(layerId)

    val query =
      attributeName match {
        case Some(name) =>
          QueryBuilder.delete()
            .from(attributeKeyspace, attributeTable)
            .where(eqs("layerId", layerIdString(layerId)))
            .and(eqs("name", name))
        case None =>
          QueryBuilder.delete()
            .from(attributeKeyspace, attributeTable)
            .where(eqs("layerId", layerIdString(layerId)))
      }

    session.execute(query)

    attributeName match {
      case Some(attribute) => clearCache(layerId, attribute)
      case None => clearCache(layerId)
    }
  }

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = instance.withSessionDo { session =>
    val query =
      QueryBuilder.select.column("value")
        .from(attributeKeyspace, attributeTable)
        .where(eqs("layerId", layerIdString(layerId)))
        .and(eqs("name", attributeName))

    val values = session.execute(query)

    val size = values.getAvailableWithoutFetching
    if (size == 0) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if (size > 1) {
      throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      val (_, result) = values.one.getString("value").parseJson.convertTo[(LayerId, T)]
      result
    }
  }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] = instance.withSessionDo { session =>
    val query = QueryBuilder.select("value")
      .from(attributeKeyspace, attributeTable).allowFiltering()
      .where(eqs("name", QueryBuilder.bindMarker()))

    val preparedStatement = session.prepare(query)
    session.execute(preparedStatement.bind(attributeName))
      .all
      .map {
        _.getString("value").parseJson.convertTo[(LayerId, T)]
      }
      .toMap
  }

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = instance.withSessionDo { session =>
    val update =
      QueryBuilder.update(attributeKeyspace, attributeTable)
        .`with`(set("value", (layerId, value).toJson.compactPrint))
        .where(eqs("layerId", layerIdString(layerId)))
        .and(eqs("name", attributeName))

    session.execute(update)
  }

  def layerExists(layerId: LayerId): Boolean = instance.withSessionDo { session =>
    val query =
      QueryBuilder.select.column("layerId")
        .from(attributeKeyspace, attributeTable)
        .where(eqs("layerId", layerIdString(layerId)))

    session.execute(query).exists { key =>
      val List(name, zoomStr) = key.getString("layerId").split(SEP).toList
      layerId == LayerId(name, zoomStr.toInt)
    }
  }

  def delete(layerId: LayerId): Unit = delete(layerId, None)

  def delete(layerId: LayerId, attributeName: String): Unit = delete(layerId, Some(attributeName))

  def layerIds: Seq[LayerId] = instance.withSessionDo { session =>
    val query =
      QueryBuilder.select.column("layerId")
        .from(attributeKeyspace, attributeTable)

    session.execute(query).map { key =>
      val List(name, zoomStr) = key.getString("layerId").split(SEP).toList
      LayerId(name, zoomStr.toInt)
    }
      .toList
      .distinct
  }

  def availableAttributes(layerId: LayerId): Seq[String] = instance.withSessionDo { session =>
    val query =
      QueryBuilder.select("name")
        .from(attributeKeyspace, attributeTable)
        .where(eqs("layerId", layerIdString(layerId)))

    session.execute(query).map(_.getString("name")).toVector
  }
}
