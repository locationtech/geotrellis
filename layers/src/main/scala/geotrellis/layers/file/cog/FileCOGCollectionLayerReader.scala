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

package geotrellis.layers.file.cog

import java.io.File
import java.net.URI

import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layers.{LayerId, TileLayerMetadata}
import geotrellis.layers._
import geotrellis.layers.cog.{COGCollectionLayerReader, ZoomRange, Extension}
import geotrellis.layers.file.conf.FileConfig
import geotrellis.layers.file.{FileAttributeStore, KeyPathGenerator}
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging
import spray.json.JsonFormat

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from local FS.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class FileCOGCollectionLayerReader(
  val attributeStore: AttributeStore,
  val catalogPath: String,
  val defaultThreads: Int = FileCOGCollectionLayerReader.defaultThreadCount
) extends COGCollectionLayerReader[LayerId] with LazyLogging {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]]) = {
    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      KeyPathGenerator(catalogPath, s"${id.name}/${zoomRange.slug}", maxWidth) andThen (_ ++ s".$Extension")

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      getKeyPath      = getKeyPath,
      pathExists      = { new File(_).isFile },
      fullPath        = { path => new URI(s"file://$path") },
      defaultThreads  = defaultThreads
    )
  }
}

object FileCOGCollectionLayerReader {
  val defaultThreadCount: Int = FileConfig.threads.collection.readThreads

  def apply(attributeStore: AttributeStore, catalogPath: String): FileCOGCollectionLayerReader =
    new FileCOGCollectionLayerReader(attributeStore, catalogPath)

  def apply(catalogPath: String): FileCOGCollectionLayerReader =
    apply(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore): FileCOGCollectionLayerReader =
    apply(attributeStore, attributeStore.catalogPath)
}
