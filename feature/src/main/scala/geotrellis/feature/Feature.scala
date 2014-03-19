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

trait Feature[G <: Geometry, D] { val geom: G ; val data: D }

case class PointFeature[D](geom: Point, data: D) extends Feature[Point,D]
case class LineFeature[D](geom: Line, data: D) extends Feature[Line,D]
case class PolygonFeature[D](geom: Polygon, data: D) extends Feature[Polygon,D]
case class MultiPointFeature[D](geom: MultiPoint, data: D) extends Feature[MultiPoint,D]
case class MultiLineFeature[D](geom: MultiLine, data: D) extends Feature[MultiLine,D]
case class MultiPolygonFeature[D](geom: MultiPolygon, data: D) extends Feature[MultiPolygon,D]
case class GeometryCollectionFeature[D](geom: GeometryCollection, data: D) extends Feature[GeometryCollection,D]
