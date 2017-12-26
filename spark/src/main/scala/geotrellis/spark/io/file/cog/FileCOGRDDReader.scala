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
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.util._
import geotrellis.spark.util.KryoWrapper

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory
import spray.json._

import java.io.File
import java.net.URI

import scala.reflect._

trait FileCOGRDDReader[V <: CellGrid] extends FileCOGRDDReaderTag[V] {
  import tiffMethods._

  def read[K: SpatialComponent: Boundable: JsonFormat: ClassTag](
    keyPath: BigInt => String,
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])]],
    numPartitions: Option[Int] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.rdd.read")
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if (baseQueryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val kwFormat = KryoWrapper(implicitly[JsonFormat[K]])

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decomposeBounds))
    else
      baseQueryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(BigInt, BigInt)]] =>
        val keyFormat = kwFormat.value

        partition flatMap { seq =>
          LayerReader.njoin[K, V](seq.toIterator, threads) { index: BigInt =>
            println(s"${keyPath(index)}.tiff")
            if (!new File(s"${keyPath(index)}.tiff").isFile) Vector()
            else {
              val uri = new URI(s"file://${keyPath(index)}.tiff")
              val baseKey = getKey[K](uri)(keyFormat)

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
        }
      }
      .groupBy(_._1)
      .map { case (key, (seq: Iterable[(K, V)])) => key -> seq.map(_._2).reduce(_ merge _) }
  }
}
