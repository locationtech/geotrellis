package geotrellis.spark.io.hbase

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds, LayerId}

import org.apache.avro.Schema
import org.apache.hadoop.hbase.client.Scan
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import scala.collection.JavaConversions._

import scala.reflect.ClassTag

object HBaseRDDReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: HBaseInstance,
    table: String,
    layerId: LayerId,
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

    val rdd: RDD[(K, V)] =
      sc.parallelize(bins, bins.size)
        .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
           val res: Iterator[Seq[(K, V)]] = for {
              rangeList <- partition // Unpack the one element of this partition, the rangeList.
              (rfrom, rto) <- rangeList
            } yield {
              val t = instance.getAdmin.getConnection.getTable(table)

              val scan = new Scan()
              scan.setStartRow(rfrom)
              scan.setStopRow(rto)
              scan.addFamily(columnFamily(layerId))
              t.getScanner(scan).iterator().flatMap { row =>
                val bytes = row.getValue(columnFamily(layerId), "")
                val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                if (filterIndexOnly) recs
                else recs.filter { row => includeKey(row._1) }
              }.toSeq
            }

          res.flatten
        }
    rdd
  }
}
