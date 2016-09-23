/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vectortile

import geotrellis.vector.Extent

// --- //

/** A high-level representation of a Vector Tile. At its simplest, a Tile is
  * just a collection of Layers. We opt to expose each Layer name at the Tile
  * level, as the keys of a [[Map]]. This way, if the layer names are known by
  * the user ahead of time, they can search through the Tile quickly.
  *
  * Traditionally, VectorTiles are encoded as Protobuf data, which this library
  * provides a codec for. However, by making this top-level type a trait, we
  * are able to define alternative backends (GeoJson, for instance. Yet unimplemented.).
  *
  * See [[geotrellis.vectortile.protobuf.ProtobufTile]] for more information
  * on how to decode and encode VectorTiles.
  *
  */
trait VectorTile {
  /** Every Layer in this Tile, with its name as a lookup key. */
  val layers: Map[String, Layer]

  /** The [[Extent]] of '''this''' Tile in some CRS.
    * A Tile's extent can be easily found from its Key and [[LayoutDefinition]]:
    *
    * {{{
    * val key: SpatialKey = ...
    * val layout: LayoutDefinition = ...
    *
    * val tileExtent: Extent = layout.mapTransform(key)
    * }}}
    */
  val tileExtent: Extent
}
