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

package geotrellis.layers.hadoop

import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.hadoop.conf.HadoopConfig
import geotrellis.layers.hadoop.formats.FilterMapFileInputFormat
import geotrellis.layers.util.IOUtils

import com.github.blemale.scaffeine.{Cache, Scaffeine}

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io._


class HadoopCollectionReader(maxOpenFiles: Int) {

  val readers: Cache[Path, MapFile.Reader] =
    Scaffeine()
      .recordStats()
      .maximumSize(maxOpenFiles.toLong)
      .removalListener[Path, MapFile.Reader] { case (_, v, _) => v.close() }
      .build[Path, MapFile.Reader]

  private def predicate(row: (Path, BigInt, BigInt), index: BigInt): Boolean =
    (index >= row._2) && ((index <= row._3) || (row._3 == -1))

  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](path: Path,
    conf: Configuration,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    indexFilterOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = HadoopCollectionReader.defaultThreads
  ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toIterator

    val codec = KeyValueRecordCodec[K, V]

    val pathRanges: Vector[(Path, BigInt, BigInt)] =
      FilterMapFileInputFormat.layerRanges(path, conf)

    IOUtils.parJoin[K, V](indexRanges, threads){ index: BigInt =>
      val valueWritable = pathRanges
        .find(row => predicate(row, index))
        .map { case (p, _, _) =>
          readers.get(p, path => new MapFile.Reader(path, conf))
        }
        .map(_.get(new BigIntWritable(index.toByteArray), new BytesWritable()).asInstanceOf[BytesWritable])
        .getOrElse { println(s"Index ${index} not found."); null }

      if (valueWritable == null) Vector.empty
      else {
        val items = AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), valueWritable.getBytes)(codec)
        if (indexFilterOnly) items
        else items.filter { row => includeKey(row._1) }
      }
    }
  }
}

object HadoopCollectionReader {
  val defaultThreads: Int = HadoopConfig.threads.collection.readThreads
  def apply(maxOpenFiles: Int = 16) = new HadoopCollectionReader(maxOpenFiles)
}
