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

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path
import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import com.typesafe.config.ConfigFactory
import com.github.blemale.scaffeine.{Cache, Scaffeine}

import java.util.concurrent.Executors

class HadoopCollectionReader(maxOpenFiles: Int) {

  val readers: Cache[Path, MapFile.Reader] =
    Scaffeine()
      .recordStats()
      .maximumSize(maxOpenFiles.toLong)
      .removalListener[Path, MapFile.Reader] { case (_, v, _) => v.close() }
      .build[Path, MapFile.Reader]

  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](path: Path,
    conf: Configuration,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    indexFilterOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.hadoop.threads.collection.read")): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toIterator

    val codec = KeyValueRecordCodec[K, V]

    val pathRanges: Vector[(Path, Long, Long)] =
      FilterMapFileInputFormat.layerRanges(path, conf)

    LayerReader.njoin[K, V](indexRanges, threads){ index: Long =>
      val valueWritable = pathRanges
        .find { row => index >= row._2 && index <= row._3 }
        .map { case (p, _, _) =>
          readers.get(path, _ => new MapFile.Reader(p, conf))
        }
        .map(_.get(new LongWritable(index), new BytesWritable()).asInstanceOf[BytesWritable])
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
  def apply(maxOpenFiles: Int = 16) = new HadoopCollectionReader(maxOpenFiles)
}
