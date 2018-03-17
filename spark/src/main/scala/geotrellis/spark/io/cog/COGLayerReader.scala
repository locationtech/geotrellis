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
import geotrellis.raster.io.geotiff.reader.GeoTiffReader

import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import spray.json._

import scala.reflect._

import java.util.concurrent.Executors
import java.util.ServiceLoader
import java.net.URI

trait COGLayerReader[ID] extends Serializable {
  def defaultNumPartitions: Int

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[TileLayerMetadata[K]]

  def baseRead[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](
     id: ID,
     tileQuery: LayerQuery[K, TileLayerMetadata[K]],
     numPartitions: Int,
     getKeyPath: (ZoomRange, Int) => BigInt => String,
     pathExists: String => Boolean, // check the path above exists
     fullPath: String => URI, // add an fs prefix
     defaultThreads: Int
   )(implicit sc: SparkContext,
              getByteReader: URI => ByteReader,
              idIdentity: ID => LayerId
   ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]]

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](id: ID): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    read(id, defaultNumPartitions)

  def reader[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ]: Reader[ID, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new Reader[ID, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {
      def read(id: ID): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
        COGLayerReader.this.read[K, V](id)
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
}
