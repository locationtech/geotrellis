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

package geotrellis.spark.io.hadoop

import geotrellis.tiling.{Boundable, Bounds, KeyBounds}
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.hadoop._
import geotrellis.layers.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.io.hadoop.formats.FilterMapSparkFileInputFormat
import geotrellis.spark.util.KryoWrapper

import com.typesafe.scalalogging.LazyLogging

import org.apache.avro.Schema
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object HadoopRDDReader extends LazyLogging {

  def readFully[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](path: Path, writerSchema: Option[Schema] = None)(implicit sc: SparkContext): RDD[(K, V)] = {
    val dataPath = path.suffix(HadoopCatalogConfig.SEQFILE_GLOB)

    logger.debug(s"Loading from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val codec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    sc.newAPIHadoopRDD(
      inputConf,
      classOf[SequenceFileInputFormat[BigIntWritable, BytesWritable]],
      classOf[BigIntWritable], // key class
      classOf[BytesWritable]  // value class
     )
      .flatMap { case (keyWritable, valueWritable) =>
        AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.schema), valueWritable.getBytes)(codec)
      }
  }

  def readFiltered[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](
    path: Path,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    indexFilterOnly: Boolean,
    writerSchema: Option[Schema] = None)
  (implicit sc: SparkContext): RDD[(K, V)] = {
    if(queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val dataPath = path.suffix(HadoopCatalogConfig.SEQFILE_GLOB)

    logger.debug(s"Loading from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toArray
    inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, indexRanges)

    val codec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    sc.newAPIHadoopRDD(
      inputConf,
      classOf[FilterMapSparkFileInputFormat],
      classOf[BigIntWritable],
      classOf[BytesWritable]
    )
      .flatMap { case (keyWritable, valueWritable) =>
        val items = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.schema), valueWritable.getBytes)(codec)
        if(indexFilterOnly)
          items
        else
          items.filter { row => includeKey(row._1) }
      }
  }
}
