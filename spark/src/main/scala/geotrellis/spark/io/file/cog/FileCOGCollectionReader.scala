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

package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.MergeQueue
import geotrellis.util._

import com.typesafe.config.ConfigFactory
import spray.json._

import java.io.File
import java.net.URI

import scala.reflect._

trait FileCOGCollectionReader[V <: CellGrid] extends FileCOGCollectionReaderTag[V] {
  import tiffMethods._

  def read[K: SpatialComponent: Boundable: JsonFormat: ClassTag](
    keyPath: BigInt => String,
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])]],
    numPartitions: Option[Int] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.collection.read")
  ): Seq[(K, V)] = {
    if (baseQueryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decomposeBounds))
    else
      baseQueryKeyBounds.flatMap(decomposeBounds)

    LayerReader.njoin[K, V](ranges.toIterator, threads) { index: BigInt =>
      println(s"${keyPath(index)}.tiff")
      if (!new File(s"${keyPath(index)}.tiff").isFile) Vector()
      else {
        val uri = new URI(s"file://${keyPath(index)}.tiff")
        val baseKey = getKey[K](uri)

        readDefinitions
          .get(baseKey.getComponent[SpatialKey])
          .flatMap(_.headOption)
          .map { case (spatialKey, overviewIndex, _, seq) =>
            val key = baseKey.setComponent(spatialKey)
            val tiff = readTiff(uri, overviewIndex)
            val map = seq.map { case (gb, sk) => gb -> key.setComponent(sk) }.toMap

            tileTiff(tiff, map)
          }
          .getOrElse(Vector())
      }
    }
    .groupBy(_._1)
    .map { case (key, (seq: Iterable[(K, V)])) => key -> seq.map(_._2).reduce(_ merge _) }
    .toSeq
  }
}
