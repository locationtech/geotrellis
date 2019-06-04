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

package geotrellis.spark.store

import geotrellis.tiling._
import geotrellis.layers.{AttributeStore, Reader}
import geotrellis.spark._
import geotrellis.layers.avro._
import geotrellis.util._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import cats.effect.{IO, Timer}
import cats.syntax.apply._
import spray.json._

import scala.reflect._
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.util.ServiceLoader
import java.net.URI

import geotrellis.layers.{LayerId, Metadata}

trait LayerReader[ID] {
  def defaultNumPartitions: Int
  val attributeStore: AttributeStore

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[M]

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID): RDD[(K, V)] with Metadata[M] =
    read(id, defaultNumPartitions)

  def reader[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ]: Reader[ID, RDD[(K, V)] with Metadata[M]] =
    new Reader[ID, RDD[(K, V)] with Metadata[M]] {
      def read(id: ID): RDD[(K, V)] with Metadata[M] =
        LayerReader.this.read[K, V, M](id)
    }
}

object LayerReader {
  /**
   * Produce FilteringLayerReader instance based on URI description.
   * Find instances of [[LayerReaderProvider]] through Java SPI.
   */
  def apply(attributeStore: AttributeStore, layerReaderUri: URI)(implicit sc: SparkContext): FilteringLayerReader[LayerId] = {
    import scala.collection.JavaConverters._
    ServiceLoader.load(classOf[LayerReaderProvider])
      .iterator().asScala
      .find(_.canProcess(layerReaderUri))
      .getOrElse(throw new RuntimeException(s"Unable to find LayerReaderProvider for $layerReaderUri"))
      .layerReader(layerReaderUri, attributeStore, sc)
  }

  /**
   * Produce FilteringLayerReader instance based on URI description.
   * Find instances of [[LayerReaderProvider]] through Java SPI.
   */
  def apply(attributeStoreUri: URI, layerReaderUri: URI)(implicit sc: SparkContext): FilteringLayerReader[LayerId] =
    apply(attributeStore = AttributeStore(attributeStoreUri), layerReaderUri)

  /**
   * Produce FilteringLayerReader instance based on URI description.
   * Find instances of [[LayerReaderProvider]] through Java SPI.
   * Required [[AttributeStoreProvider]] instance will be found from the same URI.
   */
  def apply(uri: URI)(implicit sc: SparkContext): FilteringLayerReader[LayerId] =
    apply(attributeStoreUri = uri, layerReaderUri = uri)

  def apply(attributeStore: AttributeStore, layerReaderUri: String)(implicit sc: SparkContext): FilteringLayerReader[LayerId] =
    apply(attributeStore, new URI(layerReaderUri))

  def apply(attributeStoreUri: String, layerReaderUri: String)(implicit sc: SparkContext): FilteringLayerReader[LayerId] =
    apply(new URI(attributeStoreUri), new URI(layerReaderUri))

  def apply(uri: String)(implicit sc: SparkContext): FilteringLayerReader[LayerId] =
    apply(new URI(uri))
}
