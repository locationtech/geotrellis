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

import org.log4s._

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal

import scala.collection.JavaConverters._

class CassandraLayerDeleter(val attributeStore: AttributeStore, instance: CassandraInstance) extends LayerDeleter[LayerId] {
  @transient private[this] lazy val logger = getLogger

  def delete(id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[CassandraLayerHeader](id)
      instance.withSessionDo { session =>
        val squery = QueryBuilder
          .selectFrom(header.keyspace, header.tileTable)
          .column("key").allowFiltering()
          .whereColumn("name").isEqualTo(literal(id.name))
          .whereColumn("zoom").isEqualTo(literal(id.zoom))
          .build()

        val dquery = QueryBuilder
          .deleteFrom(header.keyspace, header.tileTable)
          .whereColumn("key").isEqualTo(QueryBuilder.bindMarker())
          .whereColumn("name").isEqualTo(literal(id.name))
          .whereColumn("zoom").isEqualTo(literal(id.zoom))
          .build()

        val statement = session.prepare(dquery)

        session.execute(squery).all().asScala.map { entry =>
          session.execute(statement.bind(entry.getBigInteger("key")))
        }
      }
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    } finally {
      attributeStore.delete(id)
    }
  }
}

object CassandraLayerDeleter {
  def apply(attributeStore: CassandraAttributeStore, instance: CassandraInstance): CassandraLayerDeleter =
    new CassandraLayerDeleter(attributeStore, instance)

  def apply(attributeStore: CassandraAttributeStore): CassandraLayerDeleter =
    new CassandraLayerDeleter(attributeStore, attributeStore.instance)

  def apply(instance: CassandraInstance): CassandraLayerDeleter =
    apply(CassandraAttributeStore(instance), instance)
}
