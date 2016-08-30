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

import geotrellis.vector._
import geotrellis.vectortile.protobuf.Value

// --- //

/** A high-level representation of a Vector Tile. At its simplest, a Tile is
  * just a collection of Layers. We opt to expose each Layer name at the Tile
  * level, as the keys of a [[Map]]. This way, if the layer names are known by
  * the user ahead of time, they can search through the Tile quickly.
  *
  * Traditionally, VectorTiles are encoded as Protobuf data, which this library
  * provides a codec for. However, by making this top-level type a trait, we
  * are able to define alternative backends (GeoJson, for instance).
  *
  * See [[geotrellis.vectortile.protobuf.ProtobufTile]] for more information
  * on how to decode and encode VectorTiles.
  *
  */
trait VectorTile {
  /** Every Layer in this Tile, with its name as a lookup key. */
  val layers: Map[String, Layer]
}

/** A layer, which could contain any number of Features of any Geometry type.
  * Here, "Feature" and "Geometry" refer specifically to the Geotrellis classes
  * of the same names.
  */
trait Layer {
  /** The layer's name. */
  def name: String

  /** The width/height of this Layer's coordinate grid. By default this is 4096,
    * as per the VectorTile specification.
    *
    * Not to be confused with a [[geotrellis.vector.Extent]], which represents
    * some projected area on a map.
    */
  def extent: Int

  /** Every Point Feature in this Layer. */
  def points: Seq[Feature[Point, Map[String, Value]]]
  /** Every MultiPoint Feature in this Layer. */
  def multiPoints: Seq[Feature[MultiPoint, Map[String, Value]]]
  /** Every Line Feature in this Layer. */
  def lines: Seq[Feature[Line, Map[String, Value]]]
  /** Every MultiLine Feature in this Layer. */
  def multiLines: Seq[Feature[MultiLine, Map[String, Value]]]
  /** Every Polygon Feature in this Layer. */
  def polygons: Seq[Feature[Polygon, Map[String, Value]]]
  /** Every MultiPolygon Feature in this Layer. */
  def multiPolygons: Seq[Feature[MultiPolygon, Map[String, Value]]]

  /** All Features of Single and Multi Geometries. */
  def features: Seq[Feature[Geometry, Map[String, Value]]]
}
