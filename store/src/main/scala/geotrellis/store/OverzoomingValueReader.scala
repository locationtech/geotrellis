/*
 * Copyright 2019 Azavea
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

package geotrellis.store

import geotrellis.layer.{SpatialComponent, SpatialKey, TileLayerMetadata, ZoomedLayoutScheme}
import geotrellis.raster.resample.{ResampleMethod, TileResampleMethods}
import geotrellis.raster.{CellGrid, RasterExtent}
import geotrellis.store.avro.AvroRecordCodec
import geotrellis.util._

import io.circe._

import scala.reflect.ClassTag

trait OverzoomingValueReader extends ValueReader[LayerId] {
  def overzoomingReader[
    K: AvroRecordCodec: Decoder: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: AvroRecordCodec: * => TileResampleMethods[V]
  ](layerId: LayerId, resampleMethod: ResampleMethod): Reader[K, V] = new Reader[K, V] {
    val LayerId(layerName, requestedZoom) = layerId
    val maxAvailableZoom = attributeStore.availableZoomLevels(layerName).max
    val metadata = attributeStore.readMetadata[TileLayerMetadata[K]](LayerId(layerName, maxAvailableZoom))

    val layoutScheme = ZoomedLayoutScheme(metadata.crs, metadata.tileRows)
    val requestedMaptrans = layoutScheme.levelForZoom(requestedZoom).layout.mapTransform
    val maxMaptrans = metadata.mapTransform

    lazy val baseReader = reader[K, V](layerId)
    lazy val maxReader = reader[K, V](LayerId(layerName, maxAvailableZoom))

    def read(key: K): V =
      if (requestedZoom <= maxAvailableZoom) {
        baseReader.read(key)
      } else {
        val maxKey = {
          val srcSK = key.getComponent[SpatialKey]
          val denom = math.pow(2, requestedZoom - maxAvailableZoom).toInt
          key.setComponent[SpatialKey](SpatialKey(srcSK._1 / denom, srcSK._2 / denom))
        }

        val toResample = maxReader.read(maxKey)

        toResample.resample(
          maxMaptrans.keyToExtent(maxKey.getComponent[SpatialKey]),
          RasterExtent(requestedMaptrans.keyToExtent(key.getComponent[SpatialKey]), toResample.cols, toResample.rows),
          resampleMethod
        )
      }
  }
}
