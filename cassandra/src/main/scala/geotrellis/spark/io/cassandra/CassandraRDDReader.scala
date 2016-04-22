package geotrellis.spark.io.cassandra

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

object CassandraRDDReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: CassandraInstance,
    table: String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if (queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    val ranges = if (queryKeyBounds.length > 1)
        MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
      else
        queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    val query = QueryBuilder.select("value")
      .from(instance.keyspace, table)
      .where(eqs("key", QueryBuilder.bindMarker()))
      .toString

    val rdd: RDD[(K, V)] =
      sc.parallelize(bins, bins.size)
        .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
          instance.withSession { session =>
            val statement = session.prepare(query)

            val tileSeq: Iterator[Seq[(K, V)]] =
              for {
                rangeList <- partition // Unpack the one element of this partition, the rangeList.
                range <- rangeList
                index <- range._1 to range._2
              } yield {
                // mb to use iterator there?
                val row = session.execute(statement.bind(index.asInstanceOf[java.lang.Long]))
                if (row.nonEmpty) {
                  val bytes = row.one().getBytes("value").array()
                  val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)

                  if (filterIndexOnly)
                    recs
                  else
                    recs.filter { row => includeKey(row._1) }
                } else {
                  Seq.empty
                }
              }

            /** Close partition session; is there a better way to do it? */
            (tileSeq ++ Iterator({
              session.closeAsync(); session.getCluster.closeAsync(); Seq.empty[(K, V)]
            })).flatten
          }
        }

    rdd
  }
}
