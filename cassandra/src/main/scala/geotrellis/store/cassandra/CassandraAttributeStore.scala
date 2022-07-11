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

package geotrellis.store.cassandra

import geotrellis.store._
import geotrellis.store.cassandra.conf.CassandraConfig
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.api.core.`type`.DataTypes
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import cats.syntax.either._

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
        .withPartitionKey("layerName", DataTypes.TEXT)
        .withClusteringColumn("layerZoom", DataTypes.INT)
        .withClusteringColumn("name", DataTypes.TEXT)
        .withColumn("value", DataTypes.TEXT)
        .build()
    )
  }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit = instance.withSessionDo { session =>
    val query =
      attributeName match {
        case Some(name) =>
          QueryBuilder.deleteFrom(attributeKeyspace, attributeTable)
            .whereColumn("layerName").isEqualTo(literal(layerId.name))
            .whereColumn("layerZoom").isEqualTo(literal(layerId.zoom))
            .whereColumn("name").isEqualTo(literal(name))
        case None =>
          QueryBuilder.deleteFrom(attributeKeyspace, attributeTable)
            .whereColumn("layerName").isEqualTo(literal(layerId.name))
            .whereColumn("layerZoom").isEqualTo(literal(layerId.zoom))
      }

    session.execute(query.build())

    attributeName match {
      case Some(attribute) => clearCache(layerId, attribute)
      case None => clearCache(layerId)
    }
  }

  def read[T: Decoder](layerId: LayerId, attributeName: String): T = instance.withSessionDo { session =>
    val query =
      QueryBuilder.selectFrom(attributeKeyspace, attributeTable)
        .column("value")
        .whereColumn("layerName").isEqualTo(literal(layerId.name))
        .whereColumn("layerZoom").isEqualTo(literal(layerId.zoom))
        .whereColumn("name").isEqualTo(literal(attributeName))
        .build()

    val values = session.execute(query)

    val size = values.getAvailableWithoutFetching
    if (size == 0) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if (size > 1) {
      throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      val (_, result) = parse(values.one.getString("value")).flatMap(_.as[(LayerId, T)]).valueOr(throw _)
      result
    }
  }

  def readAll[T: Decoder](attributeName: String): Map[LayerId, T] = instance.withSessionDo { session =>
    val query =
      QueryBuilder.selectFrom(attributeKeyspace, attributeTable)
        .column("value")
        .allowFiltering()
        .whereColumn("name").isEqualTo(QueryBuilder.bindMarker())
        .build()

    val preparedStatement = session.prepare(query)
    session.execute(preparedStatement.bind(attributeName))
      .all
      .asScala
      .map { row =>
        parse(row.getString("value")).flatMap(_.as[(LayerId, T)]).valueOr(throw _)
      }
      .toMap
  }

  def write[T: Encoder](layerId: LayerId, attributeName: String, value: T): Unit = instance.withSessionDo { session =>
    val update =
      QueryBuilder.update(attributeKeyspace, attributeTable)
        .setColumn("value", literal((layerId, value).asJson.noSpaces))
        .whereColumn("layerName").isEqualTo(literal(layerId.name))
        .whereColumn("layerZoom").isEqualTo(literal(layerId.zoom))
        .whereColumn("name").isEqualTo(literal(attributeName))
        .build()

    session.execute(update)
  }

  def layerExists(layerId: LayerId): Boolean = instance.withSessionDo { session =>
    val query =
      QueryBuilder.selectFrom(attributeKeyspace, attributeTable)
        .columns("layerName", "layerZoom")
        .whereColumn("layerName").isEqualTo(literal(layerId.name))
        .whereColumn("layerZoom").isEqualTo(literal(layerId.zoom))
        .whereColumn("name").isEqualTo(literal(AttributeStore.Fields.metadata))
        .build()

    session.execute(query).asScala.exists { key =>
      val (name, zoom) = key.getString("layerName") -> key.getInt("layerZoom")
      layerId == LayerId(name, zoom)
    }
  }

  def delete(layerId: LayerId): Unit = delete(layerId, None)

  def delete(layerId: LayerId, attributeName: String): Unit = delete(layerId, Some(attributeName))

  def layerIds: Seq[LayerId] = instance.withSessionDo { session =>
    val query = QueryBuilder
      .selectFrom(attributeKeyspace, attributeTable)
      .columns("layerName", "layerZoom")
      .build()

    session.execute(query).asScala.map { key =>
      val (name, zoom) = key.getString("layerName") -> key.getInt("layerZoom")
      LayerId(name, zoom)
    }
    .toList
    .distinct
  }

  def availableAttributes(layerId: LayerId): Seq[String] = instance.withSessionDo { session =>
    val query =
      QueryBuilder
        .selectFrom(attributeKeyspace, attributeTable)
        .column("name")
        .whereColumn("layerName").isEqualTo(literal(layerId.name))
        .whereColumn("layerZoom").isEqualTo(literal(layerId.zoom))
        .build()

    session.execute(query).asScala.map(_.getString("name")).toVector
  }

  override def availableZoomLevels(layerName: String): Seq[Int] = instance.withSessionDo { session =>
    val query =
      QueryBuilder
        .selectFrom(attributeKeyspace, attributeTable)
        .column("layerZoom")
        .whereColumn("layerName").isEqualTo(literal(layerName))
        .build()

    session.execute(query).asScala.map(_.getInt("layerZoom")).toList.distinct
  }
}
