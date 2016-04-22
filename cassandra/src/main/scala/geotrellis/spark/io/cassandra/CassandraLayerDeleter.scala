package geotrellis.spark.io.cassandra

import geotrellis.spark.LayerId
import geotrellis.spark.io._

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}

class CassandraLayerDeleter(val attributeStore: AttributeStore, instance: CassandraInstance) extends LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val header = try {
      attributeStore.readHeader[CassandraLayerHeader](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    instance.withSessionDo { session =>
      session.execute(
        QueryBuilder.delete()
          .from(instance.keyspace, header.tileTable)
          .where(eqs("name", id.name))
          .and(eqs("zoom", id.zoom))
      )
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
