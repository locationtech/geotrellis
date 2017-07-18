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

package geotrellis

/**
  * ''This package is experimental. Expect API flux.''
  *
  * Invented by [[https://www.mapbox.com/ Mapbox]], VectorTiles are a combination of the
  * ideas of finite-sized tiles and vector geometries. Mapbox maintains the
  * official implementation spec for VectorTile codecs. The specification is free
  * and open source.
  *
  * VectorTiles are advantageous over raster tiles in that:
  *   - They are typically smaller to store
  *   - They can be easily transformed (rotated, etc.) in real time
  *   - They allow for continuous (as opposed to step-wise) zoom in Slippy Maps.
  *
  * Raw VectorTile data is stored in the protobuf format. Any codec implementing
  * [[https://github.com/mapbox/vector-tile-spec/tree/master/2.1 the spec]] must
  * decode and encode data according to
  * [[https://github.com/mapbox/vector-tile-spec/blob/master/2.1/vector_tile.proto this ''.proto'' schema]].
  *
  * ==What is this package?==
  * ''geotrellis-vectortile'' is a high-performance implementation of
  * '''Version 2.1''' of the VectorTile spec. It features:
  *   - Decoding of '''Version 2''' VectorTiles from Protobuf byte data into useful GeoTrellis types.
  *   - Lazy decoding of Geometries. Only parse what you need!
  *
  * ==Using this Package==
  *
  * ===Modules===
  * Users of this library need only pay attention to [[geotrellis.vectortile]].
  * Any classes in the ''internal.*'' submodules are unique to the machinery
  * of VectorTile {de,en}coding, and can be safely ignored.
  *
  * ===Types===
  *
  * The central type is the [[VectorTile]] class. Its companion object can
  * be used to construct VectorTiles from raw byte data:
  *
  * {{{
  * import geotrellis.spark.SpatialKey
  * import geotrellis.spark.tiling.LayoutDefinition
  * import geotrellis.vector.Extent
  * import geotrellis.vectortile.VectorTile
  *
  * val bytes: Array[Byte] = ...  // from some `.mvt` file
  * val key: SpatialKey = ...  // preknown
  * val layout: LayoutDefinition = ...  // preknown
  * val tileExtent: Extent = layout.mapTransform(key)
  *
  * /* Decode Protobuf bytes. */
  * val tile: VectorTile = VectorTile.fromBytes(bytes, tileExtent)
  *
  * /* Encode a VectorTile back into bytes. */
  * val encodedBytes: Array[Byte] = tile.toBytes
  *
  * }}}
  *
  * The ''V*'' types form a small [[https://en.wikipedia.org/wiki/Sum_type sum type]]
  * and are used to represent usually untyped Feature-level metadata. This metadata
  * is equivalent to a JSON Object, where String keys index values of any type.
  * A Scala Map requires more rigidity (for the better), and so we use
  * [[geotrellis.vectortile.Value]] to guarantee type safety.
  *
  * ==Implementation Assumptions==
  * This particular implementation of the VectorTile spec makes the following
  * assumptions:
  *   - Geometries are implicitly encoded in ''some'' Coordinate Reference
  *     system. That is, there is no such thing as a "projectionless" VectorTile.
  *     When decoding a VectorTile, we must provide a GeoTrellis [[Extent]] that
  *     represents the Tile's area on a map.
  *     With this, the grid coordinates stored in the VectorTile's Geometry are
  *     shifted from their
  *     original [0,4096] range to actual world coordinates in the Extent's CRS.
  *   - The `id` field in VectorTile Features doesn't matter.
  *   - `UNKNOWN` geometries are safe to ignore.
  *   - If a VectorTile `geometry` list marked as `POINT` has only one pair
  *     of coordinates, it will be decoded as a GeoTrellis `Point`. If it has
  *     more than one pair, it will be decoded as a `MultiPoint`. Likewise for
  *     the `LINESTRING` and `POLYGON` types. A complaint has been made about
  *     the spec regarding this, and future versions may include a difference
  *     between single and multi geometries.
  *
  * @author cwoodbury@azavea.com
  * @version 2.1
  */
package object vectortile {}
