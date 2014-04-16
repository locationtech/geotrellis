/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import spray.json._

abstract class Feature[D] {
  type G <: Geometry
  val geom: G ; val data: D
}

case class PointFeature[D](geom: Point, data: D) extends Feature[D] {type G = Point}
case class LineFeature[D](geom: Line, data: D) extends Feature[D] {type G = Line}
case class PolygonFeature[D](geom: Polygon, data: D) extends Feature[D] {type G = Polygon}
case class MultiPointFeature[D](geom: MultiPoint, data: D) extends Feature[D] {type G = MultiPoint}
case class MultiLineFeature[D](geom: MultiLine, data: D) extends Feature[D] {type G = MultiLine}
case class MultiPolygonFeature[D](geom: MultiPolygon, data: D) extends Feature[D] {type G = MultiPolygon}
case class GeometryCollectionFeature[D](geom: GeometryCollection, data: D) extends Feature[D] {type G = GeometryCollection}