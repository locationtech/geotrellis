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
import geotrellis.vectortile.protobuf.ProtobufGeom

// --- //

package object protobuf {
  implicit class ProtobufPoint(p: Point) extends ProtobufGeom[Point] {
    def fromCommands(cmds: Seq[Command]): Point = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufMultiPoint(mp: MultiPoint) extends ProtobufGeom[MultiPoint] {
    def fromCommands(cmds: Seq[Command]): MultiPoint = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufLine(l: Line) extends ProtobufGeom[Line] {
    def fromCommands(cmds: Seq[Command]): Line = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufMultiLine(ml: MultiLine) extends ProtobufGeom[MultiLine] {
    def fromCommands(cmds: Seq[Command]): MultiLine = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufPolygon(p: Polygon) extends ProtobufGeom[Polygon] {
    def fromCommands(cmds: Seq[Command]): Polygon = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufMultiPolygon(mp: MultiPolygon) extends ProtobufGeom[MultiPolygon] {
    def fromCommands(cmds: Seq[Command]): MultiPolygon = ???

    def toCommands: Seq[Command] = ???
  }
}
