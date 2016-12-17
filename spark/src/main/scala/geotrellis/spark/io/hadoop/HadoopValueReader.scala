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

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.util.cache._

import org.apache.hadoop.fs._
import org.apache.hadoop.io._
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import spray.json._

import scala.collection.immutable._
import scala.reflect.ClassTag

class HadoopValueReader(
  val attributeStore: AttributeStore,
  conf: Configuration,
  maxOpenFiles: Int = 16
) extends ValueReader[LayerId] {

  val readers = new LRUCache[(LayerId, Path), MapFile.Reader](maxOpenFiles.toLong, {x => 1l}) {
    override def evicted(reader: MapFile.Reader) = reader.close()
  }

  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[HadoopLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)
    val codec = KeyValueRecordCodec[K, V]

    val ranges: Vector[(Path, Long, Long)] =
      FilterMapFileInputFormat.layerRanges(header.path, conf)

    def read(key: K): V = {
      val index: Long = keyIndex.toIndex(key)
      val valueWritable: BytesWritable =
      ranges
          .find{ row =>
            index >= row._2 && index <= row._3
          }
        .map { case (path, _, _) =>
            readers.getOrInsert((layerId, path), new MapFile.Reader(path, conf))
        }
        .getOrElse(throw new ValueNotFoundError(key, layerId))
          .get(new LongWritable(index), new BytesWritable())
          .asInstanceOf[BytesWritable]

      if (valueWritable == null) throw new ValueNotFoundError(key, layerId)
      AvroEncoder
        .fromBinary(writerSchema, valueWritable.getBytes)(codec)
        .find { row => row._1 == key }
        .getOrElse(throw new ValueNotFoundError(key, layerId))
        ._2
    }
  }
}

object HadoopValueReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    attributeStore: AttributeStore,
    layerId: LayerId
  )(implicit sc: SparkContext): Reader[K, V] =
    new HadoopValueReader(attributeStore, sc.hadoopConfiguration).reader[K, V](layerId)

  def apply(attributeStore: HadoopAttributeStore): HadoopValueReader =
    new HadoopValueReader(attributeStore, attributeStore.hadoopConfiguration)

  def apply(rootPath: Path)
    (implicit sc: SparkContext): HadoopValueReader =
    apply(HadoopAttributeStore(rootPath))

  def apply(rootPath: Path, conf: Configuration): HadoopValueReader =
    apply(HadoopAttributeStore(rootPath, conf))
}
