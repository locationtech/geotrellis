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

package geotrellis.spark.io.accumulo

import geotrellis.tiling.{Boundable, KeyBounds}
import geotrellis.layers.accumulo.AccumuloInstance
import geotrellis.layers.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.layers.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.util.KryoWrapper

import org.apache.accumulo.core.client.mapreduce.{AccumuloInputFormat, InputFormatBase}
import org.apache.accumulo.core.data.{Range => AccumuloRange, Value, Key}
import org.apache.accumulo.core.util.{Pair => AccumuloPair}
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import scala.collection.JavaConverters._
import scala.reflect.ClassTag


object AccumuloRDDReader {
  def read[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    table: String,
    columnFamily: Text,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[AccumuloRange],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None
  )(implicit sc: SparkContext, instance: AccumuloInstance): RDD[(K, V)] = {
    if(queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val codec = KryoWrapper(KeyValueRecordCodec[K, V])
    val includeKey = (key: K) => queryKeyBounds.includeKey(key)

    val job = Job.getInstance(sc.hadoopConfiguration)
    instance.setAccumuloConfig(job)
    InputFormatBase.setInputTableName(job, table)

    val ranges = queryKeyBounds.flatMap(decomposeBounds).asJava
    InputFormatBase.setRanges(job, ranges)
    InputFormatBase.fetchColumns(job, List(new AccumuloPair(columnFamily, null: Text)).asJava)
    InputFormatBase.setBatchScan(job, true)

    val kwWriterSchema = KryoWrapper(writerSchema)
    sc.newAPIHadoopRDD(
      job.getConfiguration,
      classOf[AccumuloInputFormat],
      classOf[Key],
      classOf[Value])
    .map { case (_, value) =>
      AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.value.schema), value.get)(codec.value)
    }
    .flatMap { pairs: Vector[(K, V)] =>
      if(filterIndexOnly)
        pairs
      else
        pairs.filter { pair => includeKey(pair._1) }
    }
  }
}
