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

import vector_tile.{vector_tile => vt}

// --- //

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
sealed trait Value {
  def unval: vt.Tile.Value
}

case class St(v: String) extends Value {
  def unval: vt.Tile.Value = vt.Tile.Value().withStringValue(v)
}

case class Fl(v: Float) extends Value {
  def unval: vt.Tile.Value = vt.Tile.Value().withFloatValue(v)
}

case class Do(v: Double) extends Value {
  def unval: vt.Tile.Value = vt.Tile.Value().withDoubleValue(v)
}

case class I64(v: Long) extends Value {
  def unval: vt.Tile.Value = vt.Tile.Value().withIntValue(v)
}

case class W64(v: Long) extends Value {
  def unval: vt.Tile.Value = vt.Tile.Value().withUintValue(v)
}

case class S64(v: Long) extends Value {
  def unval: vt.Tile.Value = vt.Tile.Value().withSintValue(v)
}

case class Bo(v: Boolean) extends Value {
  def unval: vt.Tile.Value = vt.Tile.Value().withBoolValue(v)
}
