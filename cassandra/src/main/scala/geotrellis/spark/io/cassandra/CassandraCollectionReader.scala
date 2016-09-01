package geotrellis.spark.io.cassandra

import geotrellis.spark.{Boundable, KeyBounds, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.MergeQueue
import geotrellis.spark.util.KryoWrapper

import org.apache.avro.Schema
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

object CassandraCollectionReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: CassandraInstance,
    keyspace: String,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.cassandra.threads.collection.read")
  ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val query = QueryBuilder.select("value")
      .from(keyspace, table)
      .where(eqs("key", QueryBuilder.bindMarker()))
      .and(eqs("name", layerId.name))
      .and(eqs("zoom", layerId.zoom))
      .toString

    instance.withSessionDo { session =>
      val statement = session.prepare(query)

      LayerReader.njoin[K, V](ranges.toIterator, threads){ index: Long =>
        val row = session.execute(statement.bind(index.asInstanceOf[java.lang.Long]))
        if (row.nonEmpty) {
          val bytes = row.one().getBytes("value").array()
          val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
          if (filterIndexOnly) recs
          else recs.filter { row => includeKey(row._1) }
        } else Vector.empty
      }
    }: Seq[(K, V)]
  }
}
