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

package geotrellis.spark.store.file

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.layers._
import geotrellis.layers._
import geotrellis.layers.file.{FileAttributeStore, FileLayerHeader, KeyPathGenerator, LayerPath}
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.index._
import geotrellis.layers.merge.Mergable
import geotrellis.layers.{LayerId, Metadata}
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.merge._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.spark.rdd.RDD

import spray.json._

import scala.reflect._
import java.io.File


/**
  * Handles writing Raster RDDs and their metadata to a filesystem.
  *
  * @tparam K                Type of RDD Key (ex: SpatialKey)
  * @tparam V                Type of RDD Value (ex: Tile or MultibandTile )
  * @tparam M                Type of Metadata associated with the RDD[(K,V)]
  *
  * @param catalogPath  The root directory of this catalog.
  * @param keyPrefix         File prefix to write the raster to
  * @param keyIndexMethod    Method used to convert RDD keys to SFC indexes
  * @param attributeStore    AttributeStore to be used for storing raster metadata
  */
class FileLayerWriter(
    val attributeStore: AttributeStore,
    catalogPath: String
) extends LayerWriter[LayerId] with LazyLogging {

  // Layer Updating
  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M]
  ): Unit = {
    update(id, rdd, None)
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    mergeFunc: (V, V) => V
  ): Unit = {
    update(id, rdd, Some(mergeFunc))
  }

  private def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    mergeFunc: Option[(V, V) => V]
  ): Unit = {
    validateUpdate[FileLayerHeader, K, V, M](id, rdd.metadata) match {
      case Some(LayerAttributes(header, metadata, keyIndex, writerSchema)) =>
        val path = header.path
        val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
        val keyPath = KeyPathGenerator(catalogPath, path, keyIndex, maxWidth)
        val layerPath = new File(catalogPath, path).getAbsolutePath

        logger.info(s"Writing update for layer ${id} to $path")

        attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, writerSchema)
        FileRDDWriter.update[K, V](rdd, layerPath, keyPath, Some(writerSchema), mergeFunc)
        
      case None =>
        logger.warn(s"Skipping update with empty bounds for $id.")
    }
  }

  // Layer Writing
  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](layerId: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val catalogPathFile = new File(catalogPath)

    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    require(!attributeStore.layerExists(layerId), s"$layerId already exists")

    val path = LayerPath(layerId)
    val metadata = rdd.metadata
    val header =
      FileLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = path
      )

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, path, keyIndex, maxWidth)
    val layerPath = new File(catalogPath, path).getAbsolutePath

    try {
      attributeStore.writeLayerAttributes(layerId, header, metadata, keyIndex, schema)

      logger.info(s"Saving RDD ${layerId.name} to ${catalogPath}")
      FileRDDWriter.write(rdd, layerPath, keyPath)
    } catch {
      case e: Exception => throw new LayerWriteError(layerId).initCause(e)
    }
  }
}

object FileLayerWriter {
  def apply(attributeStore: FileAttributeStore): FileLayerWriter =
    new FileLayerWriter(
      attributeStore,
      attributeStore.catalogPath
    )

  def apply(catalogPath: String): FileLayerWriter =
    apply(FileAttributeStore(catalogPath))

}
