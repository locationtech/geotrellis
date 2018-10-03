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

import reflect.runtime.universe._
import org.locationtech.jts.{geom => jts}

import GeomFactory._

/** Companion object to [[GeometryCollection]] */
object GeometryCollection {
  implicit def jtsToGeometryCollection(gc: jts.GeometryCollection): GeometryCollection =
    apply(gc)

  def apply(points: Seq[Point] = Seq(), lines: Seq[Line] = Seq(), polygons: Seq[Polygon] = Seq(),
             multiPoints: Seq[MultiPoint] = Seq(),
             multiLines: Seq[MultiLine] = Seq(),
             multiPolygons: Seq[MultiPolygon] = Seq(),
             geometryCollections: Seq[GeometryCollection] = Seq()
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

  def unapply(gc: GeometryCollection):
      Some[(Seq[Point], Seq[Line], Seq[Polygon],
            Seq[MultiPoint], Seq[MultiLine], Seq[MultiPolygon],
            Seq[GeometryCollection])] =
    Some((gc.points, gc.lines, gc.polygons,
          gc.multiPoints, gc.multiLines, gc.multiPolygons,
          gc.geometryCollections))
}

/** A collection of geometries (itself a [[Geometry]]) */
class GeometryCollection(
    val points: Seq[Point],
    val lines: Seq[Line],
    val polygons: Seq[Polygon],
    val multiPoints: Seq[MultiPoint],
    val multiLines: Seq[MultiLine],
    val multiPolygons: Seq[MultiPolygon],
    val geometryCollections: Seq[GeometryCollection],
    val jtsGeom: jts.GeometryCollection
  ) extends Geometry {

  def geometries: Seq[Geometry] =
    points ++ lines ++ polygons ++ multiPoints ++ multiLines ++ multiPolygons ++ geometryCollections

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): GeometryCollection = {
    val geom = jtsGeom.clone.asInstanceOf[jts.GeometryCollection]
    geom.normalize
    GeometryCollection(geom)
  }

  def getAll[G <: Geometry: TypeTag]: Seq[G] =
    typeOf[G] match {
      case x if x <:< typeOf[Point] => points.asInstanceOf[Seq[G]]
      case x if x <:< typeOf[Line] => lines.asInstanceOf[Seq[G]]
      case x if x <:< typeOf[Polygon] => polygons.asInstanceOf[Seq[G]]
      case x if x <:< typeOf[MultiPoint] => multiPoints.asInstanceOf[Seq[G]]
      case x if x <:< typeOf[MultiLine] => multiLines.asInstanceOf[Seq[G]]
      case x if x <:< typeOf[MultiPolygon] => multiPolygons.asInstanceOf[Seq[G]]
      case x if x <:< typeOf[GeometryCollection] => geometryCollections.asInstanceOf[Seq[G]]
    }

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
}
