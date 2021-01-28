/*
 * Copyright 2019 Azavea
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

import geotrellis.vector.{
    Point,
    MultiPoint,
    LineString,
    MultiLineString,
    Polygon,
    MultiPolygon
}
import cats.Monoid

/** Container case class for collections of [[MVTFeature]]s
  *
  * This is separated for ease of use when reading geometries from
  * places. With the `Monoid` instance for `MVTFeatures`, each geometry
  * can be lifted into a seq in the relevant attribute, and then
  * the separate `MVTFeatures` case classes can all be combined.
  */
final case class MVTFeatures(
  /** Every Point Feature in this Layer. */
  points: Seq[MVTFeature[Point]],
  /** Every MultiPoint Feature in this Layer. */
  multiPoints: Seq[MVTFeature[MultiPoint]],
  /** Every Line Feature in this Layer. */
  lines: Seq[MVTFeature[LineString]],
  /** Every MultiLine Feature in this Layer. */
  multiLines: Seq[MVTFeature[MultiLineString]],
  /** Every Polygon Feature in this Layer. */
  polygons: Seq[MVTFeature[Polygon]],
  /** Every MultiPolygon Feature in this Layer. */
  multiPolygons: Seq[MVTFeature[MultiPolygon]]
)

object MVTFeatures {
  implicit val monoidMVTFeatures: Monoid[MVTFeatures] = new Monoid[MVTFeatures] {
    def empty: MVTFeatures = MVTFeatures(Nil, Nil, Nil, Nil, Nil, Nil)

    def combine(x: MVTFeatures, y: MVTFeatures): MVTFeatures = MVTFeatures(
      x.points ++ y.points,
      x.multiPoints ++ y.multiPoints,
      x.lines ++ y.lines,
      x.multiLines ++ y.multiLines,
      x.polygons ++ y.polygons,
      x.multiPolygons ++ y.multiPolygons
    )
  }
}
