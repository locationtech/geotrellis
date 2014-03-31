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

package geotrellis
import com.vividsolutions.jts.{ geom => jts }

package object feature {
  implicit class WrappedJtsGeometry(geom:jts.Geometry) {
    private def flattenToPolygon(g:jts.Geometry):List[jts.Polygon] =
      g match {
        case g if g.isEmpty => List()
        case g: jts.GeometryCollection =>
          (0 until g.getNumGeometries).flatMap { i =>
            flattenToPolygon(g.getGeometryN(i))
          }.toList
        case l: jts.LineString => List()
        case p: jts.Point => List()
        case p: jts.Polygon => List(p)
      }

    def asPolygonSet() = flattenToPolygon(geom)
  }

  implicit def pointToFeature(p:jts.Point): Point[Int] = 
    Point(p, 0)

  implicit def lineStringToFeature(l:jts.LineString): LineString[Int] =
    LineString(l, 0)

  implicit def polyToFeature(p:jts.Polygon): Polygon[Int] =
    Polygon(p, 0)

  implicit def multiPointToFeature(mp:jts.MultiPoint): MultiPoint[Int] =
    MultiPoint(mp, 0)

  implicit def multiLineStringToFeature(mls:jts.MultiLineString): MultiLineString[Int] =
    MultiLineString(mls, 0)

  implicit def mutliPolygonToFeature(mp:jts.MultiPolygon): MultiPolygon[Int] =
    MultiPolygon(mp, 0)

  implicit def geometryCollectionToFeature(gc:jts.GeometryCollection): GeometryCollection[Int] =
    GeometryCollection(gc, 0)
}
