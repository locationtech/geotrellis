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

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}
import scala.collection.mutable

class GeometryCollection(
    val points: Set[Point],
    val lines: Set[Line],
    val polygons: Set[Polygon],
    val multiPoints: Set[MultiPoint],
    val multiLines: Set[MultiLine],
    val multiPolygons: Set[MultiPolygon],
    val geometryCollections: Set[GeometryCollection],
    val jtsGeom: jts.GeometryCollection
  ) extends Geometry {

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): GeometryCollection = { jtsGeom.normalize ; GeometryCollection(jtsGeom) }

  lazy val area: Double =
    jtsGeom.getArea

  override def equals(that: Any): Boolean = {
    that match {
      case that: GeometryCollection =>
        //this allows to match equality ignoring the order or membership
        this.points == that.points &&
        this.lines == that.lines &&
        this.polygons == that.polygons &&
        this.multiLines == that.multiLines &&
        this.multiPolygons == that.multiPolygons &&
        this.geometryCollections == that.geometryCollections
      case _ => false
    }
  }

  override def hashCode(): Int  =
    jtsGeom.hashCode()

  override def toString: String = jtsGeom.toString
}

object GeometryCollection {
  implicit def jtsToGeometryCollection(gc: jts.GeometryCollection): GeometryCollection =
    apply(gc)

  def apply(points: Set[Point] = Set(), lines: Set[Line] = Set(), polygons: Set[Polygon] = Set(),
             multiPoints: Set[MultiPoint] = Set(),
             multiLines: Set[MultiLine] = Set(),
             multiPolygons: Set[MultiPolygon] = Set(),
             geometryCollections: Set[GeometryCollection] = Set()
           ): GeometryCollection =
  {
    val jtsGeom = factory.createGeometryCollection(
      (points ++ lines ++ polygons ++ multiPoints ++ multiLines ++ multiPolygons ++ geometryCollections)
        .map(_.jtsGeom).toArray
    )
    new GeometryCollection(points, lines, polygons, multiPoints, multiLines, multiPolygons, geometryCollections, jtsGeom)
  }

  def apply(geoms: Traversable[Geometry]): GeometryCollection = {
    val builder = new GeometryCollectionBuilder()
    builder ++= geoms
    builder.result()
  }

  def apply(gc: jts.GeometryCollection): GeometryCollection = {
    val builder = new GeometryCollectionBuilder()
    for (i <- 0 until gc.getNumGeometries){
      builder += gc.getGeometryN(i)
    }
    builder.result()
  }

  def unapply(gc: GeometryCollection): Some[(Set[Point], Set[Line], Set[Polygon])] =
    Some((gc.points, gc.lines, gc.polygons))
}
