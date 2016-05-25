package geotrellis.spark.io.cassandra

import geotrellis.spark.LayerId
import geotrellis.spark.io._

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}

import scala.collection.JavaConversions._

class CassandraLayerDeleter(val attributeStore: AttributeStore, instance: CassandraInstance) extends LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val header = try {
      attributeStore.readHeader[CassandraLayerHeader](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    instance.withSessionDo { session =>
      val squery = QueryBuilder.select("key")
        .from(header.keyspace, header.tileTable).allowFiltering()
        .where(eqs("name", id.name))
        .and(eqs("zoom", id.zoom))

      val dquery = QueryBuilder.delete()
        .from(header.keyspace, header.tileTable)
        .where(eqs("key", QueryBuilder.bindMarker()))
        .and(eqs("name", id.name))
        .and(eqs("zoom", id.zoom))

      val statement = session.prepare(dquery)

      session.execute(squery).all().map { entry =>
        session.execute(statement.bind(entry.getLong("key").asInstanceOf[java.lang.Long]))
      }
    }

    attributeStore.delete(id)
  }
}

object CassandraLayerDeleter {
  def apply(attributeStore: AttributeStore, instance: CassandraInstance): CassandraLayerDeleter =
    new CassandraLayerDeleter(attributeStore, instance)

  def apply(attributeStore: CassandraAttributeStore): CassandraLayerDeleter =
    new CassandraLayerDeleter(attributeStore, attributeStore.instance)

  def apply(instance: CassandraInstance): CassandraLayerDeleter =
    apply(CassandraAttributeStore(instance), instance)
}
