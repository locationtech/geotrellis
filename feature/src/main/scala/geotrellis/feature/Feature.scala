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
object PointFeature { implicit def feature2Geom[D](f: PointFeature[D]): Point = f.geom }

case class LineFeature[D](geom: Line, data: D) extends Feature[D] {type G = Line}
object LineFeature { implicit def feature2Geom[D](f: LineFeature[D]): Line = f.geom }

case class PolygonFeature[D](geom: Polygon, data: D) extends Feature[D] {type G = Polygon}
object PolygonFeature { implicit def feature2Geom[D](f: PolygonFeature[D]): Polygon = f.geom }

case class MultiPointFeature[D](geom: MultiPoint, data: D) extends Feature[D] {type G = MultiPoint}
object MultiPointFeature { implicit def feature2Geom[D](f: MultiPointFeature[D]): MultiPoint = f.geom }

case class MultiLineFeature[D](geom: MultiLine, data: D) extends Feature[D] {type G = MultiLine}
object MultiLineFeature { implicit def feature2Geom[D](f: MultiLineFeature[D]): MultiLine = f.geom }

case class MultiPolygonFeature[D](geom: MultiPolygon, data: D) extends Feature[D] {type G = MultiPolygon}
object MultiPolygonFeature { implicit def feature2Geom[D](f: MultiPolygonFeature[D]): MultiPolygon = f.geom }

case class GeometryCollectionFeature[D](geom: GeometryCollection, data: D) extends Feature[D] {type G = GeometryCollection}
object GeometryCollectionFeature { implicit def feature2Geom[D](f: GeometryCollectionFeature[D]): GeometryCollection = f.geom }
