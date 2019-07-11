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
import spire.syntax.cfor._

trait LineStringConstructors {

  def apply(points: (Double, Double)*)(implicit d: DummyImplicit): jts.LineString =
    apply(points)

  def apply(points: Traversable[(Double, Double)])(implicit d: DummyImplicit): jts.LineString =
    apply(points.map { case (x,y) => Point(x,y) })

  def apply(points: jts.Point*): jts.LineString =
    apply(points.toList)

  def apply(coords: jts.Coordinate*)(implicit d: DummyImplicit, e: DummyImplicit): jts.LineString =
    apply(GeomFactory.factory.getCoordinateSequenceFactory.create(coords.toArray))

  def apply(coords: Traversable[jts.Coordinate]): jts.LineString =
    apply(GeomFactory.factory.getCoordinateSequenceFactory.create(coords.toArray))

  def apply(coords: jts.CoordinateSequence): jts.LineString =
    GeomFactory.factory.createLineString(coords)

  def apply(points: Traversable[jts.Point])(implicit ev: DummyImplicit, ev2: DummyImplicit): jts.LineString = {
    if (points.size < 2) {
      sys.error("Invalid line: Requires 2 or more points.")
    }

    val pointArray = points.toArray
    val coords = Array.ofDim[jts.Coordinate](pointArray.size)
    cfor(0)(_ < pointArray.size, _ + 1) { i =>
      coords(i) = pointArray(i).getCoordinate
    }

    GeomFactory.factory.createLineString(coords)
  }
}

object LineString extends LineStringConstructors
