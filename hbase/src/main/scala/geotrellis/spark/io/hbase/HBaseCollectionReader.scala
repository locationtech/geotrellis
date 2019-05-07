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

package geotrellis.spark.io.hbase

import geotrellis.layers.LayerId
import geotrellis.tiling.{Boundable, KeyBounds}
import geotrellis.layers.io.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.layers.io.index.MergeQueue
import geotrellis.spark.util.KryoWrapper
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.{FilterList, MultiRowRangeFilter, PrefixFilter}
import org.apache.avro.Schema

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object HBaseCollectionReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: HBaseInstance,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None
  ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) // Avro Schema is not Serializable

    val ranges: Seq[(BigInt, BigInt)] = if (queryKeyBounds.length > 1)
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
        scanner.iterator().asScala.flatMap { row =>
          val bytes = row.getValue(HBaseRDDWriter.tilesCF, "")
          val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
          if (filterIndexOnly) recs
          else recs.filter { row => includeKey(row._1) }
        } toVector: Seq[(K, V)]
      } finally scanner.close()
    }
  }
}
