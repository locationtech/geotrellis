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

package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.util._

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

  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
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
