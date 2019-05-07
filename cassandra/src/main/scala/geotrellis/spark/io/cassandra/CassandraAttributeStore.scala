/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.conf.CassandraConfig
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{set, eq => eqs}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.DataType._
import geotrellis.layers.LayerId
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConverters._

object CassandraAttributeStore {
  def apply(instance: CassandraInstance, attributeKeyspace: String, attributeTable: String): CassandraAttributeStore =
    new CassandraAttributeStore(instance, attributeKeyspace, attributeTable)

  def apply(instance: CassandraInstance): CassandraAttributeStore =
    apply(instance, CassandraConfig.keyspace, CassandraConfig.catalog)
}

class CassandraAttributeStore(val instance: CassandraInstance, val attributeKeyspace: String, val attributeTable: String) extends DiscreteLayerAttributeStore {

  //create the attribute table if it does not exist
  instance.withSessionDo { session =>
    instance.ensureKeyspaceExists(attributeKeyspace, session)
    session.execute(
      SchemaBuilder.createTable(attributeKeyspace, attributeTable).ifNotExists()
        .addPartitionKey("layerName", text)
        .addClusteringColumn("layerZoom", cint)
        .addClusteringColumn("name", text)
        .addColumn("value", text)
    )
  }

  private def fetch(layerId: Option[LayerId], attributeName: String): ResultSet = instance.withSessionDo { session =>
    val query =
      layerId match {
        case Some(id) =>
          QueryBuilder.select.column("value")
            .from(attributeKeyspace, attributeTable)
            .where(eqs("layerName", id.name))
            .and(eqs("layerZoom", id.zoom))
            .and(eqs("name", attributeName))
        case None =>
          QueryBuilder.select.column("value")
            .from(attributeKeyspace, attributeTable)
            .where(eqs("name", attributeName))
      }

    session.execute(query)
  }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit = instance.withSessionDo { session =>
    val query =
      attributeName match {
        case Some(name) =>
          QueryBuilder.delete()
            .from(attributeKeyspace, attributeTable)
            .where(eqs("layerName", layerId.name))
            .and(eqs("layerZoom", layerId.zoom))
            .and(eqs("name", name))
        case None =>
          QueryBuilder.delete()
            .from(attributeKeyspace, attributeTable)
            .where(eqs("layerName", layerId.name))
            .and(eqs("layerZoom", layerId.zoom))
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
        .where(eqs("layerName", layerId.name))
        .and(eqs("layerZoom", layerId.zoom))
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
    val query = QueryBuilder.select.column("value")
      .from(attributeKeyspace, attributeTable).allowFiltering()
      .where(eqs("name", QueryBuilder.bindMarker()))

    val preparedStatement = session.prepare(query)
    session.execute(preparedStatement.bind(attributeName))
      .all
      .asScala
      .map { _.getString("value").parseJson.convertTo[(LayerId, T)] }
      .toMap
  }

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = instance.withSessionDo { session =>
    val update =
      QueryBuilder.update(attributeKeyspace, attributeTable)
        .`with`(set("value", (layerId, value).toJson.compactPrint))
        .where(eqs("layerName", layerId.name))
        .and(eqs("layerZoom", layerId.zoom))
        .and(eqs("name", attributeName))

    session.execute(update)
  }

  def layerExists(layerId: LayerId): Boolean = instance.withSessionDo { session =>
    val query =
      QueryBuilder.select("layerName", "layerZoom")
        .from(attributeKeyspace, attributeTable)
        .where(eqs("layerName", layerId.name))
        .and(eqs("layerZoom", layerId.zoom))
        .and(eqs("name", AttributeStore.Fields.metadata))

    session.execute(query).asScala.exists { key =>
      val (name, zoom) = key.getString("layerName") -> key.getInt("layerZoom")
      layerId == LayerId(name, zoom)
    }
  }

  def delete(layerId: LayerId): Unit = delete(layerId, None)

  def delete(layerId: LayerId, attributeName: String): Unit = delete(layerId, Some(attributeName))

  def layerIds: Seq[LayerId] = instance.withSessionDo { session =>
    val query = QueryBuilder.select("layerName", "layerZoom").from(attributeKeyspace, attributeTable)

    session.execute(query).asScala.map { key =>
      val (name, zoom) = key.getString("layerName") -> key.getInt("layerZoom")
      LayerId(name, zoom)
    }
    .toList
    .distinct
  }

  def availableAttributes(layerId: LayerId): Seq[String] = instance.withSessionDo { session =>
    val query =
      QueryBuilder.select.column("name")
        .from(attributeKeyspace, attributeTable)
        .where(eqs("layerName", layerId.name))
        .and(eqs("layerZoom", layerId.zoom))

    session.execute(query).asScala.map(_.getString("name")).toVector
  }

  override def availableZoomLevels(layerName: String): Seq[Int] = instance.withSessionDo { session =>
    val query =
      QueryBuilder.select.column("layerZoom")
        .from(attributeKeyspace, attributeTable)
        .where(eqs("layerName", layerName))

    session.execute(query).asScala.map { _.getInt("layerZoom") }.toList.distinct
  }
}
