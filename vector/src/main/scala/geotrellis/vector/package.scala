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

package geotrellis


import org.locationtech.jts.{geom => jts}

package object vector extends SeqMethods
    with reproject.Implicits
    with triangulation.Implicits
    with voronoi.Implicits
    with io.json.Implicits
    with io.wkt.Implicits
    with io.wkb.Implicits
    with methods.Implicits {

  type Point = jts.Point
  type LineString = jts.LineString
  type Polygon = jts.Polygon
  type MultiPoint = jts.MultiPoint
  type MultiLineString = jts.MultiLineString
  type MultiPolygon = jts.MultiPolygon
  type Geometry = jts.Geometry
  type GeometryCollection = jts.GeometryCollection

  type PointFeature[+D] = Feature[Point, D]
  type LineStringFeature[+D] = Feature[LineString, D]
  type PolygonFeature[+D] = Feature[Polygon, D]
  type MultiPointFeature[+D] = Feature[MultiPoint, D]
  type MultiLineStringFeature[+D] = Feature[MultiLineString, D]
  type MultiPolygonFeature[+D] = Feature[MultiPolygon, D]
  type GeometryCollectionFeature[+D] = Feature[GeometryCollection, D]

  implicit class ProjectGeometry[G <: Geometry](g: G) {
    /** Upgrade Geometry to Projected[Geometry] */
    def withSRID(srid: Int) = Projected(g, srid)
  }

  implicit val pointIsZeroDimensional: ZeroDimensional[Point] = new ZeroDimensional[Point] {}
  implicit val multiPointIsZeroDimensional: ZeroDimensional[MultiPoint] = new ZeroDimensional[MultiPoint] {}
  implicit val lineStringIsOneDimensional: OneDimensional[LineString] = new OneDimensional[LineString] {}
  implicit val multiLineStringIsOneDimensional: OneDimensional[MultiLineString] = new OneDimensional[MultiLineString] {}
  implicit val polygonIsTwoDimensional: TwoDimensional[Polygon] = new TwoDimensional[Polygon] {}
  implicit val multiPolygonIsTwoDimensional: TwoDimensional[MultiPolygon] = new TwoDimensional[MultiPolygon] {}
}
