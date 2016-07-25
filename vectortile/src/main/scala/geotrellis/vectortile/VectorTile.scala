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
  * level. This way, if the layer names are known by the user ahead of time,
  * they can search through the Tile quickly.
  */
trait VectorTile {
  val layers: Map[String, Layer]
}

/** A layer, which could contain any number of Features of any Geometry type. */
trait Layer {
  type Data = Map[String, Value]  // Temporary.

  def name: String
  def extent: Int

  def points: Seq[Feature[Point, Data]]
  def multiPoints: Seq[Feature[MultiPoint, Data]]
  def lines: Seq[Feature[Line, Data]]
  def multiLines: Seq[Feature[MultiLine, Data]]
  def polygons: Seq[Feature[Polygon, Data]]
  def multiPolygons: Seq[Feature[MultiPolygon, Data]]

//  def allGeometries: Seq[Feature[Geometry, Data]]
}
