package geotrellis.spark.io.hbase

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.MergeQueue
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds, LayerId}

import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.{FilterList, MultiRowRangeFilter, PrefixFilter}
import org.apache.avro.Schema

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

object HBaseCollectionReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: HBaseInstance,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None
  ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) // Avro Schema is not Serializable

    val ranges: Seq[(Long, Long)] = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val scan = new Scan()
    scan.addFamily(HBaseRDDWriter.tilesCF)
    scan.setFilter(
      new FilterList(
        new PrefixFilter(HBaseRDDWriter.layerIdString(layerId)),
        new MultiRowRangeFilter(
          java.util.Arrays.asList(ranges.map { case (start, stop) =>
            new MultiRowRangeFilter.RowRange(
              HBaseKeyEncoder.encode(layerId, start), true,
              HBaseKeyEncoder.encode(layerId, stop), true
            )
          }: _*)
        )
      )
    )

    instance.withTableConnectionDo(table) { tableConnection =>
      val scanner = tableConnection.getScanner(scan)
      try {
        scanner.iterator().flatMap { row =>
          val bytes = row.getValue(HBaseRDDWriter.tilesCF, "")
          val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
          if (filterIndexOnly) recs
          else recs.filter { row => includeKey(row._1) }
        } toVector: Seq[(K, V)]
      } finally scanner.close()
    }
  }
}
