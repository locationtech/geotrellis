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

package geotrellis.vector

import org.locationtech.jts.{geom => jts}

import geotrellis.vector.GeomFactory._

import scala.collection.GenTraversable

trait PolygonConstructors {
  def apply(exterior: Point*)(implicit d: DummyImplicit): Polygon =
    apply(LineString(exterior), Set())

  def apply(exterior: Seq[Point]): Polygon =
    apply(LineString(exterior), Set())

  def apply(exterior: jts.Coordinate*)(implicit d: DummyImplicit, e: DummyImplicit): Polygon =
    apply(LineString(exterior), Set())

  def apply(exterior: Seq[jts.Coordinate])(implicit d: DummyImplicit, e: DummyImplicit, f: DummyImplicit): Polygon =
    apply(LineString(exterior), Set())

  def apply(exterior: (Double, Double)*)(implicit d: DummyImplicit, e: DummyImplicit, f: DummyImplicit, g: DummyImplicit): Polygon =
    apply(LineString(exterior)(d), Set())

  def apply(exterior: Seq[(Double, Double)])(implicit d: DummyImplicit, e: DummyImplicit, f: DummyImplicit, g: DummyImplicit, h: DummyImplicit): Polygon =
    apply(LineString(exterior)(d), Set())

  def apply(exterior: LineString): Polygon =
    apply(exterior, Set())

  def apply(exterior: jts.CoordinateSequence): Polygon =
    factory.createPolygon(exterior)

  def apply(exterior: LineString, holes: LineString*): Polygon =
    apply(exterior, holes)

  def apply(exterior: jts.CoordinateSequence, holes: GenTraversable[jts.CoordinateSequence]): Polygon =
    apply(factory.createLinearRing(exterior), holes.map(factory.createLinearRing))

  def apply(exterior: LineString, holes: GenTraversable[LineString]): Polygon = {
    if(!exterior.isClosed) {
      sys.error(s"Cannot create a polygon with unclosed exterior: $exterior")
    }

    if(exterior.getNumPoints < 4) {
      sys.error(s"Cannot create a polygon with exterior with fewer than 4 points: $exterior")
    }

    val extGeom = factory.createLinearRing(exterior.getCoordinateSequence)

    val holeGeoms = (
      for (hole <- holes) yield {
        if (!hole.isClosed) {
          sys.error(s"Cannot create a polygon with an unclosed hole: $hole")
        } else {
          if (hole.getNumPoints < 4) {
            sys.error(s"Cannot create a polygon with a hole with fewer than 4 points: $hole")
          } else {
            factory.createLinearRing(hole.getCoordinateSequence)
          }
        }
      }).toArray

    apply(extGeom, holeGeoms)
  }

  def apply(exterior: jts.LinearRing, holes: GenTraversable[jts.LinearRing]): Polygon =
    factory.createPolygon(exterior, holes.toArray)
}

object Polygon extends PolygonConstructors
