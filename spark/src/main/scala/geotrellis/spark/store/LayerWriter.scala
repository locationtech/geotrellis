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
import geotrellis.layers._
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.index._
import geotrellis.layers.merge.Mergable
import geotrellis.spark._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.avro._
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect.ClassTag
import java.util.ServiceLoader
import java.net.URI


trait LayerWriter[ID] {
  val attributeStore: AttributeStore

  // Layer Updating

  /** Validate update metadata before delegating to update function with updated LayerAttributes.
    * The update function is expected to handle the saving of updated attributes and values.
    */
  protected[geotrellis]
  def validateUpdate[
    H: JsonFormat,
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec,
    M: Component[?, Bounds[K]]: Mergable: JsonFormat
  ](id: LayerId, updateMetadata: M): Option[LayerAttributes[H, M, K]] = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    updateMetadata.getComponent[Bounds[K]] match {
      case updateBounds: KeyBounds[K] =>
        val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
          attributeStore.readLayerAttributes[H, M, K](id)
        } catch {
          case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
        }

        if (!(keyIndex.keyBounds contains updateBounds))
          throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

        val updateSchemaHash = SchemaNormalization.parsingFingerprint64(KeyValueRecordCodec[K, V].schema)
        val writerSchemaHash = SchemaNormalization.parsingFingerprint64(writerSchema)
        if (updateSchemaHash != writerSchemaHash)
          throw new LayerUpdateError(id, "Update Avro record schema does not match existing schema.")

        // Data extent could have grown
        val updatedMetadata: M = metadata.merge(updateMetadata)
        val updatedAttributes = LayerAttributes(header, updatedMetadata, keyIndex, writerSchema)

        Some(updatedAttributes)

      case EmptyBounds =>
        None
    }
  }

  /** Update persisted layer, merging existing value with updated value.
    *
    * The layer metadata may change as result of the update to reflect added values.
    *
    * The method will throw if:
    *  - Specified layer does not exist
    *  - Change the Avro schema of records is detected
    *  - Update RDD [[Bounds]] are outside of layer index [[Bounds]]
    *
    * Updates with empty [[Bounds]] will be ignored.
    */
  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M], mergeFunc: (V, V) => V = { (existing: V, updating: V) => updating }): Unit

  /** Update persisted layer without checking for possible.
    *
    * ```Warning```: using this method may result in data loss.
    * Unlike the [[LayerWriter.update]] this method will not check for existing
    * records before writing the update. Use this as optimiation when you are
    * certain that update will not overlap with existing records.
    *
    * Additional care is needed in cases where [[KeyIndex]] may map multiple,
    * distinct, values of K to single index. This is likely with spatio-temproral layers.
    * In these cases overwrite will replace the whole record, possibly overwriting
    * (K,V) pairs with K is not contained in update RDD.
    *
    * The method will throw if:
    *  - Specified layer does not exist
    *  - Change the Avro schema of records is detected
    *  - Update RDD [[Bounds]] are outside of layer index [[Bounds]]
    *
    * Updates with empty [[Bounds]] will be ignored.
    */
  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M]): Unit

  // Layer Writing
  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit =
    layer.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        _write[K, V, M](id, layer, keyIndex)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit =
    layer.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        val keyIndex = keyIndexMethod.createIndex(keyBounds)
        _write[K, V, M](id, layer, keyIndex)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](keyIndexMethod: KeyIndexMethod[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndexMethod)
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](keyIndex: KeyIndex[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndex)
    }
}

object LayerWriter extends LazyLogging {

  /**
   * Produce LayerWriter instance based on URI description.
   * Find instances of [[LayerWriterProvider]] through Java SPI.
   */
  def apply(attributeStore: AttributeStore, layerWriterUri: URI): LayerWriter[LayerId] = {
    import scala.collection.JavaConverters._
    ServiceLoader.load(classOf[LayerWriterProvider])
      .iterator().asScala
      .find(_.canProcess(layerWriterUri))
      .getOrElse(throw new RuntimeException(s"Unable to find LayerWriterProvider for $layerWriterUri"))
      .layerWriter(layerWriterUri, attributeStore)
  }

  /**
   * Produce LayerReader instance based on URI description.
   * Find instances of [[LayerWriterProvider]] through Java SPI.
   */
  def apply(attributeStoreUri: URI, layerWriterUri: URI): LayerWriter[LayerId] =
    apply(attributeStore = AttributeStore(attributeStoreUri), layerWriterUri)

  /**
   * Produce LayerReader instance based on URI description.
   * Find instances of [[LayerWriterProvider]] through Java SPI.
   * Required [[AttributeStoreProvider]] instance will be found from the same URI.
   */
  def apply(uri: URI): LayerWriter[LayerId] =
    apply(attributeStoreUri = uri, layerWriterUri = uri)

  def apply(attributeStore: AttributeStore, layerWriterUri: String): LayerWriter[LayerId] =
    apply(attributeStore, new URI(layerWriterUri))

  def apply(attributeStoreUri: String, layerWriterUri: String): LayerWriter[LayerId] =
    apply(new URI(attributeStoreUri), new URI(layerWriterUri))

  def apply(uri: String): LayerWriter[LayerId] =
    apply(new URI(uri))

  private[geotrellis]
  def updateRecords[K, V](mergeFunc: Option[(V, V) => V], updating: Vector[(K, V)], existing: => Vector[(K, V)]): Vector[(K, V)] = {
    mergeFunc match {
      case None =>
        updating
      case Some(_) if existing.isEmpty =>
        updating
      case Some(fn) =>
        (existing ++ updating)
          .groupBy(_._1)
          .mapValues { row =>
            val vs = row.map(_._2)
            vs.foldLeft(vs.head)(fn)
          }
          .toVector
    }
  }
}
