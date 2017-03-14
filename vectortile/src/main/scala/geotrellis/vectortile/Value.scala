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

package geotrellis.vectortile

import geotrellis.vectortile.internal.{vector_tile => vt}

// --- //

/** Feature metadata key/value Maps are completely untyped. All keys
  * and values used by Features across a common parent Layer are stored in that
  * parent. Raw Features themselves only store indices into the parent's
  * key/value lists. So, for an example MultiPoint Feature of fire hydrant locations,
  * its metadata could look like:
  *
  * {{{
  * { name: "Hydrants",
  *   colour: "red",
  *   model: 5
  * }
  *}}}
  *
  * That's fine if interpreted as JSON, but bad as Scala, as it doesn't give us
  * a clean `Map[String, ConcreteTypeHere]`. Furthermore, Features within the
  * same Layer don't have to agree on the Value type for the same key:
  *
  * {{{
  * { name: "Stop lights",
  *   colour: 1,
  *   model: "ABC-123"
  * }
  * }}}
  *
  * Nor, actually, do Layers have to agree on key sets for their Features.
  * The sealed trait `Value` here and its extensions aim to provide some
  * type safety in light of the situation described here.
  *
  */
sealed trait Value extends Serializable {
  /** Encode this Value back into a mid-level Protobuf object. */
  private[vectortile] def toProtobuf: vt.Tile.Value
}

/** A wrapper for `String` to allow all `Value` subtypes to be stored in
  * the same Map.
  */
case class VString(value: String) extends Value {
  private[vectortile] def toProtobuf: vt.Tile.Value = vt.Tile.Value().withStringValue(value)
}
/** A wrapper for `Float` to allow all `Value` subtypes to be stored in
  * the same Map.
  */
case class VFloat(value: Float) extends Value {
  private[vectortile] def toProtobuf: vt.Tile.Value = vt.Tile.Value().withFloatValue(value)
}
/** A wrapper for `Double` to allow all `Value` subtypes to be stored in
  * the same Map.
  */
case class VDouble(value: Double) extends Value {
  private[vectortile] def toProtobuf: vt.Tile.Value = vt.Tile.Value().withDoubleValue(value)
}
/** A wrapper for `Long` to allow all `Value` subtypes to be stored in
  * the same Map.
  */
case class VInt64(value: Long) extends Value {
  private[vectortile] def toProtobuf: vt.Tile.Value = vt.Tile.Value().withIntValue(value)
}
/** A wrapper for unsigned, 64-bit ints to allow all `Value` subtypes to be
  * stored in the same Map.
  */
case class VWord64(value: Long) extends Value {
  private[vectortile] def toProtobuf: vt.Tile.Value = vt.Tile.Value().withUintValue(value)
}
/** A wrapper for zig-zag encoded ints to allow all `Value` subtypes to be
  * stored in the same Map.
  */
case class VSint64(value: Long) extends Value {
  private[vectortile] def toProtobuf: vt.Tile.Value = vt.Tile.Value().withSintValue(value)
}
/** A wrapper for `Boolean` to allow all `Value` subtypes to be stored in
  * the same Map.
  */
case class VBool(value: Boolean) extends Value {
  private[vectortile] def toProtobuf: vt.Tile.Value = vt.Tile.Value().withBoolValue(value)
}
