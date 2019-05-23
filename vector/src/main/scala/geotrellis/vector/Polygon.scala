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
import scala.collection.JavaConverters._

object Polygon {
  def apply(exterior: jts.Point*)(implicit d: DummyImplicit): jts.Polygon =
    apply(LineString(exterior), Set())

  def apply(exterior: Seq[jts.Point]): jts.Polygon =
    apply(LineString(exterior), Set())

  def apply(exterior: jts.LineString): jts.Polygon =
    apply(exterior, Set())

  def apply(exterior: jts.CoordinateSequence): jts.Polygon =
    factory.createPolygon(exterior)

  def apply(exterior: jts.LineString, holes: jts.LineString*): jts.Polygon =
    apply(exterior, holes)

  def apply(exterior: jts.CoordinateSequence, holes: GenTraversable[jts.CoordinateSequence]): jts.Polygon =
    apply(factory.createLinearRing(exterior), holes.map(factory.createLinearRing))

  def apply(exterior: jts.LineString, holes: GenTraversable[jts.LineString]): jts.Polygon = {
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

  def apply(exterior: jts.LinearRing, holes: GenTraversable[jts.LinearRing]): jts.Polygon =
    factory.createPolygon(exterior, holes.toArray)
}
