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

package geotrellis.store

import geotrellis.layer.{SpaceTimeKey, SpatialKey, TileLayerMetadata}

object Implicits extends Implicits

trait Implicits extends avro.codecs.Implicits with json.Implicits {
  implicit class AttributeStoreOps(attributeStore: AttributeStore) {
    private [store] def readTileLayerMetadataErased(layerId: LayerId): TileLayerMetadata[_] = {
      val header = attributeStore.readHeader[LayerHeader](layerId)
      if(header.keyClass.contains("SpatialKey")) attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](layerId)
      else attributeStore.readMetadata[TileLayerMetadata[SpaceTimeKey]](layerId)
    }
  }
}