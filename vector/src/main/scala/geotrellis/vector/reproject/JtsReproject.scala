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

package geotrellis.vector.reproject

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.vector.GeomFactory.factory

import com.vividsolutions.jts.{geom => jts}
import spire.syntax.cfor._

/** This object contains various overloads for performing reprojections over geometries */
object JtsReproject {
  def apply(p: jts.Coordinate, transform: Transform): jts.Coordinate = {
    val (newX, newY) = transform(p.x, p.y)
    new jts.Coordinate(newX, newY, p.z)
  }

  def apply(p: jts.Coordinate, src: CRS, dest: CRS): jts.Coordinate =
    apply(p, Transform(src, dest))

  def apply(p: jts.Point, src: CRS, dest: CRS): jts.Point =
    apply(p, Transform(src, dest))

 def apply(p: jts.Point, transform: Transform): jts.Point =
   factory.createPoint(apply(p.getCoordinate, transform))

  def apply(l: jts.LineString, src: CRS, dest: CRS): jts.LineString =
    apply(l, Transform(src, dest))

  def apply(l: jts.LineString, transform: Transform): jts.LineString =
    factory.createLineString(l.getCoordinates.map { p => apply(p, transform) })

  def apply(l: jts.LinearRing, src: CRS, dest: CRS): jts.LinearRing =
    apply(l, Transform(src, dest))

  def apply(l: jts.LinearRing, transform: Transform): jts.LinearRing =
    factory.createLinearRing(l.getCoordinates.map { p => apply(p, transform) })

  def apply(p: jts.Polygon, src: CRS, dest: CRS): jts.Polygon =
    apply(p, Transform(src, dest))

  def apply(p: jts.Polygon, transform: Transform): jts.Polygon = {
    val exterior = factory.createLinearRing(p.getExteriorRing.getCoordinates.map { p => apply(p, transform) })
    val len = p.getNumInteriorRing
    val holes = Array.ofDim[jts.LinearRing](len)
    cfor(0)(_ < len, _ + 1) { i =>
      holes(i) = factory.createLinearRing(p.getInteriorRingN(i).getCoordinates.map { p => apply(p, transform) })
    }

    factory.createPolygon(exterior, holes)
  }

  def apply(mp: jts.MultiPoint, src: CRS, dest: CRS): jts.MultiPoint =
    apply(mp, Transform(src, dest))

  def apply(mp: jts.MultiPoint, transform: Transform): jts.MultiPoint =
    factory.createMultiPoint(mp.getCoordinates.map { p => apply(p, transform) })

  def apply(ml: jts.MultiLineString, src: CRS, dest: CRS): jts.MultiLineString =
    apply(ml, Transform(src, dest))

  def apply(ml: jts.MultiLineString, transform: Transform): jts.MultiLineString = {
    val len = ml.getNumGeometries
    val lines = Array.ofDim[jts.LineString](len)
    cfor(0)(_ < len, _ + 1) { i =>
      lines(i) = apply(ml.getGeometryN(i).asInstanceOf[jts.LineString], transform)
    }
    factory.createMultiLineString(lines)
  }

  def apply(mp: jts.MultiPolygon, src: CRS, dest: CRS): jts.MultiPolygon =
    apply(mp, Transform(src, dest))

  def apply(mp: jts.MultiPolygon, transform: Transform): jts.MultiPolygon = {
    val len = mp.getNumGeometries
    val lines = Array.ofDim[jts.Polygon](len)
    cfor(0)(_ < len, _ + 1) { i =>
      lines(i) = apply(mp.getGeometryN(i).asInstanceOf[jts.Polygon], transform)
    }
    factory.createMultiPolygon(lines)
  }
  def apply(gc: jts.GeometryCollection, src: CRS, dest: CRS): jts.GeometryCollection =
    apply(gc, Transform(src, dest))

  def apply(gc: jts.GeometryCollection, transform: Transform): jts.GeometryCollection = {
    val len = gc.getNumGeometries
    val lines = Array.ofDim[jts.Geometry](len)
    cfor(0)(_ < len, _ + 1) { i =>
      lines(i) = apply(gc.getGeometryN(i), transform)
    }
    factory.createGeometryCollection(lines)
  }

  def apply(g: jts.Geometry, src: CRS, dest: CRS): jts.Geometry =
    apply(g, Transform(src, dest))

  def apply(g: jts.Geometry, transform: Transform): jts.Geometry =
    g match {
      case p: jts.Point => apply(p, transform)
      case l: jts.LinearRing => apply(l, transform)
      case l: jts.LineString => apply(l, transform)
      case p: jts.Polygon => apply(p, transform)
      case mp: jts.MultiPoint => apply(mp, transform)
      case ml: jts.MultiLineString => apply(ml, transform)
      case mp: jts.MultiPolygon => apply(mp, transform)
      case gc: jts.GeometryCollection => apply(gc, transform)
    }
}
