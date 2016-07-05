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
  type Data = Map[String, Value]

  val name: String
  val extent: Int

  def points: Seq[Feature[Point, Data]]
  def multiPoints: Seq[Feature[MultiPoint, Data]]
  def lines: Seq[Feature[Line, Data]]
  def multiLines: Seq[Feature[MultiLine, Data]]
  def polygons: Seq[Feature[Polygon, Data]]
  def multiPolygons: Seq[Feature[MultiPolygon, Data]]

//  def allGeometries: Seq[Feature[Geometry, Data]]
}

/** Feature metadata key/value Maps are completely untyped. All keys
  * and values used by Features across a common parent Layer are stored in that
  * parent. Raw Features themselves only store indices into the parent's
  * key/value lists. So, for an example MultiPoint Feature of fire hydrant locations,
  * its metadata could look like:
  *
  * { name: "Hydrants",
  *   colour: "red",
  *   model: 5
  * }
  *
  * That's fine if interpreted as JSON, but bad as Scala, as it doesn't give us
  * a clean `Map[String, ConcreteTypeHere]`. Furthermore, Features within the
  * same Layer don't have to agree on the Value type for the same key:
  *
  * { name: "Stop lights",
  *   colour: 1,
  *   model: "ABC-123"
  * }
  *
  * The sealed trait `Value` here and its extensions aim to provide some
  * type safety in light of the situation described above.
  *
  */
sealed trait Value

case class St(v: String) extends Value
case class Fl(v: Float) extends Value
case class Do(v: Double) extends Value
case class I64(v: Long) extends Value
case class W64(v: Long) extends Value
case class S64(v: Long) extends Value
case class Bo(v: Boolean) extends Value
