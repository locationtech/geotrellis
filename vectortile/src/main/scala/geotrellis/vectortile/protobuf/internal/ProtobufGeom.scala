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

package geotrellis.vectortile.protobuf.internal

import geotrellis.vector.{ Geometry, MultiGeometry }

// --- //

/** An isomorphism for any Geotrellis geometry type that can convert
  * between a collection of Command Integers. As of version 2.1 of
  * the VectorTile spec, there is no explicit difference between single
  * and multi geometries. When there eventually is, this trait will have to be
  * split to provide separate instances for both the single and multi forms.
  *
  * Instances can be found in the package object.
  *
  * Usage:
  * {{{
  * implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(Command.commands(Seq(9,2,2)))
  * }}}
  */
trait ProtobufGeom[G1 <: Geometry, G2 <: MultiGeometry] {
  def fromCommands(cmds: Seq[Command]): Either[G1, G2]
  def toCommands(g: Either[G1, G2]): Seq[Command]
}
