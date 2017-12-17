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

package geotrellis.spark.io.cog

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util._
import geotrellis.raster.CellGrid

import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import spray.json._
import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import scala.reflect._

import java.util.concurrent.Executors
import java.util.ServiceLoader
import java.net.URI

trait COGLayerReader[ID] {
  def defaultNumPartitions: Int

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[M]

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID): RDD[(K, V)] with Metadata[M] =
    read(id, defaultNumPartitions)

  def reader[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ]: Reader[ID, RDD[(K, V)] with Metadata[M]] =
    new Reader[ID, RDD[(K, V)] with Metadata[M]] {
      def read(id: ID): RDD[(K, V)] with Metadata[M] =
        COGLayerReader.this.read[K, V, M](id)
    }
}

object COGLayerReader {
  /**
   * Produce FilteringCOGLayerReader instance based on URI description.
   * Find instances of [[LayerReaderProvider]] through Java SPI.
   */
  def apply(attributeStore: AttributeStore, layerReaderUri: URI)(implicit sc: SparkContext): FilteringCOGLayerReader[LayerId] = {
    import scala.collection.JavaConversions._
    ServiceLoader.load(classOf[LayerReaderProvider]).iterator()
      .find(_.canProcess(layerReaderUri))
      .getOrElse(throw new RuntimeException(s"Unable to find LayerReaderProvider for $layerReaderUri"))
      .layerReader(layerReaderUri, attributeStore, sc)
      .asInstanceOf[FilteringCOGLayerReader[LayerId]]
  }

  /**
   * Produce FilteringCOGLayerReader instance based on URI description.
   * Find instances of [[LayerReaderProvider]] through Java SPI.
   */
  def apply(attributeStoreUri: URI, layerReaderUri: URI)(implicit sc: SparkContext): FilteringCOGLayerReader[LayerId] =
    apply(attributeStore = AttributeStore(attributeStoreUri), layerReaderUri)

  /**
   * Produce FilteringCOGLayerReader instance based on URI description.
   * Find instances of [[LayerReaderProvider]] through Java SPI.
   * Required [[AttributeStoreProvider]] instance will be found from the same URI.
   */
  def apply(uri: URI)(implicit sc: SparkContext): FilteringCOGLayerReader[LayerId] =
    apply(attributeStoreUri = uri, layerReaderUri = uri)

  def apply(attributeStore: AttributeStore, layerReaderUri: String)(implicit sc: SparkContext): FilteringCOGLayerReader[LayerId] =
    apply(attributeStore, new URI(layerReaderUri))

  def apply(attributeStoreUri: String, layerReaderUri: String)(implicit sc: SparkContext): FilteringCOGLayerReader[LayerId] =
    apply(new URI(attributeStoreUri), new URI(layerReaderUri))

  def apply(uri: String)(implicit sc: SparkContext): FilteringCOGLayerReader[LayerId] =
    apply(new URI(uri))

  def njoin[K, V](
    ranges: Iterator[(Long, Long)],
    threads: Int
   )(readFunc: Long => Vector[(K, V)]): Vector[(K, V)] = {
    val pool = Executors.newFixedThreadPool(threads)

    val indices: Iterator[Long] = ranges.flatMap { case (start, end) =>
      (start to end).toIterator
    }

    val index: Process[Task, Long] = Process.unfold(indices) { iter =>
      if (iter.hasNext) {
        val index: Long = iter.next()
        Some(index, iter)
      }
      else None
    }

    val readRecord: (Long => Process[Task, Vector[(K, V)]]) = { index =>
      Process eval Task { readFunc(index) } (pool)
    }

    try {
      nondeterminism
        .njoin(maxOpen = threads, maxQueued = threads) { index map readRecord }(Strategy.Executor(pool))
        .runFoldMap(identity).unsafePerformSync
    } finally pool.shutdown()
  }
}
