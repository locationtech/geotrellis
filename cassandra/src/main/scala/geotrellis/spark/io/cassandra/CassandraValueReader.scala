package geotrellis.spark.io.cassandra

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import spray.json._

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

class CassandraValueReader(
  instance: CassandraInstance,
  val attributeStore: AttributeStore
) extends ValueReader[LayerId] {

  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[CassandraLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)
    val codec = KeyValueRecordCodec[K, V]

    def read(key: K): V = instance.withSession { session =>
      val statement = session.prepare(
        QueryBuilder.select("value")
          .from(header.keyspace, header.tileTable)
          .where(eqs("key", QueryBuilder.bindMarker()))
          .and(eqs("name", layerId.name))
          .and(eqs("zoom", layerId.zoom))
      )

      val row = session.execute(statement.bind(keyIndex.toIndex(key).asInstanceOf[java.lang.Long])).all()
      val tiles = row.map { entry =>
          AvroEncoder.fromBinary(writerSchema, entry.getBytes("value").array())(codec)
        }
        .flatMap { pairs: Vector[(K, V)] =>
          pairs.filter(pair => pair._1 == key)
        }
        .toVector

      if (tiles.isEmpty) {
        throw new TileNotFoundError(key, layerId)
      } else if (tiles.size > 1) {
        throw new LayerIOError(s"Multiple tiles(${tiles.size}) found for $key for layer $layerId")
      } else {
        tiles.head._2
      }
    }
  }
}

object CassandraValueReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    instance: CassandraInstance,
    attributeStore: AttributeStore,
    layerId: LayerId
  ): Reader[K, V] =
    new CassandraValueReader(instance, attributeStore).reader[K, V](layerId)

  def apply(instance: CassandraInstance): CassandraValueReader =
    new CassandraValueReader(
      instance = instance,
      attributeStore = CassandraAttributeStore(instance))

  def apply(attributeStore: CassandraAttributeStore): CassandraValueReader =
    new CassandraValueReader(
      instance = attributeStore.instance,
      attributeStore = attributeStore)
}
