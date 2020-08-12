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

import geotrellis.vector.GeomFactory._
import org.locationtech.jts.{geom => jts}

trait PointConstructors {
  def apply(x: Double, y: Double): jts.Point =
    factory.createPoint(new jts.Coordinate(x, y))

  def apply(t: (Double, Double)): jts.Point =
    apply(t._1, t._2)

  def apply(coord: jts.Coordinate): jts.Point =
    factory.createPoint(coord)
}

object Point extends PointConstructors
