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

import geotrellis.util.MethodExtensions

import org.locationtech.jts.geom._

import scala.reflect._

package object vector extends SeqMethods
    with reproject.Implicits
    with triangulation.Implicits
    with voronoi.Implicits
    with io.json.Implicits
    with io.wkt.Implicits
    with io.wkb.Implicits {

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

  implicit class withExtraPointMethods(val self: Point) extends MethodExtensions[Point] {
    def x(): Double = self.getCoordinate.getX
    def y(): Double = self.getCoordinate.getY
  }

  implicit class withExtraLineStringMethods(val self: LineString) extends MethodExtensions[LineString] {
    def closed(): LineString = {
      val arr = Array.ofDim[Point](self.getNumPoints + 1)

      cfor(0)(_ < arr.length, _ + 1) { i =>
        arr(i) = self.getCoordinateN(i)
      }
      arr(self.getNumPoints) = self.getCoordinateN(0)

      factory.createLineString(arr)
    }
  }

  implicit class withExtraGometryCollectionMethods(val self: GeometryCollection) extends MethodExtensions[GeometryCollection] {
    def getAll[G <: Geometry : ClassTag]: Seq[G] = {
      val lb = scala.collection.mutable.ListBuffer.empty[G]
      cfor(0)(_ < self.getNumGeometries, _ + 1){ i =>
        if (classTag[G].runtimeClass.isInstance(self.getGeometryN(i)))
          lb += self.getGeometryN(i).asInstanceOf[G]
      }
      lb.toSeq
    }
  }
}
