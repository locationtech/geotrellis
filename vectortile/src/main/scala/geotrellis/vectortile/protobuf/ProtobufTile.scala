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

package geotrellis.vectortile.protobuf

import geotrellis.vector._
import geotrellis.vectortile.{Layer, VectorTile}
import geotrellis.vectortile.protobuf.internal._
import geotrellis.vectortile.protobuf.internal.{vector_tile => vt}

// --- //

/**
  * A concrete representation of a VectorTile, as one decoded from Protobuf
  * bytes. This is the original/default type for VectorTiles.
  *
  * {{{
  * import geotrellis.vectortile.protobuf._
  *
  * val bytes: Array[Byte] = ...  // from some `.mvt` file
  * val key: SpatialKey = ...  // preknown
  * val layout: LayoutDefinition = ...  // preknown
  * val tileExtent: Extent = layout.mapTransform(key)
  *
  * val tile: VectorTile = ProtobufTile.fromBytes(bytes, tileExtent)
  * }}}
  *
  * @constructor This is not meant to be called directly. See this class's
  * companion object for the available helper methods.
  */
case class ProtobufTile(
  layers: Map[String, ProtobufLayer],
  tileExtent: Extent
) extends VectorTile {
  /** Encode this VectorTile back into a mid-level Protobuf object. */
  def toProtobuf: vt.Tile =
    vt.Tile(layers = layers.values.map(_.toProtobuf).toSeq)

  /** Encode this VectorTile back into its original form of Protobuf bytes. */
  def toBytes: Array[Byte] = toProtobuf.toByteArray
}

object ProtobufTile {
  /** Create a ProtobufTile masked as its parent trait. */
  def fromPBTile(
    tile: vt.Tile,
    tileExtent: Extent
  ): VectorTile = {

    val layers: Map[String, ProtobufLayer] = tile.layers.map({ l =>
      val pbl = LazyProtobufLayer(l, tileExtent)

      pbl.name -> pbl
    }).toMap

    new ProtobufTile(layers, tileExtent)
  }

  /** Create a [[VectorTile]] from raw Protobuf bytes.
    *
    * @param bytes  Raw Protobuf bytes from a `.mvt` file or otherwise.
    * @param tileExtent The [[Extent]] of this tile, '''not''' the global extent.
    */
  def fromBytes(
    bytes: Array[Byte],
    tileExtent: Extent
  ): VectorTile = {
    fromPBTile(vt.Tile.parseFrom(bytes), tileExtent)
  }
}
