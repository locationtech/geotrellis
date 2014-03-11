/**************************************************************************
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
 **************************************************************************/

package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}
import scala.collection.mutable

class GeometryCollection(val points: Set[Point],
                         val lines: Set[Line],
                         val polygons: Set[Polygon],
                         val jtsGeom: jts.GeometryCollection) extends Geometry {

  lazy val area: Double =
    jtsGeom.getArea

  override def equals(that: Any): Boolean = {
    that match {
      case other: GeometryCollection => jtsGeom == other.jtsGeom
      case _ => false
    }
  }

  override def hashCode(): Int  =
    jtsGeom.hashCode()
}

object GeometryCollection {

  implicit def jtsToGeometryCollection(gc: jts.GeometryCollection): GeometryCollection =
    apply(gc)

  def apply(points: Set[Point] = Set(), lines: Set[Line] = Set(), polygons: Set[Polygon] = Set()): GeometryCollection = {
    val jtsGeom = factory.createGeometryCollection((points ++ lines ++ polygons).map(_.jtsGeom).toArray)
    new GeometryCollection(points, lines, polygons, jtsGeom)
  }

  def apply(gc: jts.GeometryCollection): GeometryCollection = {
    val (points, lines, polygons) = collectGeometries(gc)
    new GeometryCollection(points, lines, polygons, gc)
  }

  def unapply(gc: GeometryCollection): Some[(Set[Point], Set[Line], Set[Polygon])] =
    Some((gc.points, gc.lines, gc.polygons))

  @inline final private 
  def collectGeometries(gc: jts.GeometryCollection): (Set[Point], Set[Line], Set[Polygon]) = {
    val points = mutable.Set[Point]()
    val lines = mutable.Set[Line]()
    val polygons = mutable.Set[Polygon]()

    val len = gc.getNumGeometries

    for(i <- 0 until len) {
      gc.getGeometryN(i) match {
        case p: jts.Point => points += p
        case mp: jts.MultiPoint => points ++= mp
        case l: jts.LineString => lines += l
        case ml: jts.MultiLineString => lines ++= ml
        case p: jts.Polygon => polygons += p
        case mp: jts.MultiPolygon => polygons ++= mp
        case gc: jts.GeometryCollection =>
          val (ps, ls, polys) = collectGeometries(gc)
          points ++= ps
          lines ++= ls
          polygons ++= polys
      }
    }

    (points.toSet, lines.toSet, polygons.toSet)
  }
}
