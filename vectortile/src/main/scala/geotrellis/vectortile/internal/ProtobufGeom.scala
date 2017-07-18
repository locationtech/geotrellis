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

package geotrellis.vectortile.internal

import geotrellis.vector.{ Geometry, MultiGeometry, Point }

// --- //

/** An isomorphism for any GeoTrellis geometry type that can convert
  * between a collection of Command Integers. As of version 2.1 of
  * the VectorTile spec, there is no explicit difference between single
  * and multi geometries. When there eventually is, this trait will have to be
  * split to provide separate instances for both the single and multi forms.
  *
  * Since we assume that all VectorTiles implicitely exist in some CRS,
  * we ask for the top-left corner of the current Tile's extent, as well
  * as its resolution in order to perform the transformation into CRS space.
  *
  * Instances of this trait can be found in the package object.
  *
  * Usage:
  * {{{
  * val topLeft: Point = ...
  * val resolution: Double = ...
  *
  * implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(Command.commands(Seq(9,2,2)), topLeft, resolution)
  * }}}
  */
private[vectortile] trait ProtobufGeom[G1 <: Geometry, G2 <: MultiGeometry] extends Serializable {
  /**
    * Decode a sequence of VectorTile [[Command]]s into a GeoTrellis
    * Geometry. Due to the reasons stated above, this may be either
    * the single or the ''Multi'' variety of the Geometry.
    *
    * This forms an isomorphism with [[toCommands]].
    *
    * @param cmds       The Commands.
    * @param topLeft    The location in the current CRS of the top-left corner of this Tile.
    * @param resolution How much of the CRS's units are covered by a single VT grid coordinate..
    */
  def fromCommands(cmds: Seq[Command], topLeft: Point, resolution: Double): Either[G1, G2]

  /**
    * Encode a GeoTrellis Geometry into a sequence of VectorTile [[Command]]s.
    *
    * This forms an isomorphism with [[fromCommands]].
    *
    * @param g          A GeoTrellis Geometry, either a `Left(Single)` or a `Right(Multi)`.
    * @param topLeft    The location in the current CRS of the top-left corner of this Tile.
    * @param resolution How much of the CRS's units are covered by a single VT grid coordinate..
    */
  def toCommands(g: Either[G1, G2], topLeft: Point, resolution: Double): Seq[Command]
}
