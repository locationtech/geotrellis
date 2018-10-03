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

import org.locationtech.jts.geom.{CoordinateSequence, TopologyException}
import org.locationtech.jts.{geom => jts}
import geotrellis.vector.GeomFactory._
import spire.syntax.cfor._

import scala.reflect.{ClassTag, classTag}

/** A trait inherited by classes which wrap a jts.Geometry */
trait Geometry extends Serializable {

  /** Return the wrapped jts Geometry */
  def jtsGeom: jts.Geometry

  /** Check the validity of this geometry */
  def isValid: Boolean =
    jtsGeom.isValid

  /** Is this Geometry empty? This is faster than checking manually like:
    * {{{
    * val mp: MultiPoint = ...
    * val ps: Array[Point] = mp.points  // `.points` is a lazy val with processing overhead
    *
    * ps.isEmpty  // possible, but mp.isEmpty is faster
    * }}}
    * It would be similar for [[MultiLine]] or [[MultiPolygon]].
    */
  def isEmpty: Boolean =
    jtsGeom.isEmpty

  /** Calculate the distance to another Geometry */
  def distance(other: Geometry): Double =
    jtsGeom.distance(other.jtsGeom)

  /** Determine whether another Geometry is within a given distance
    *
    * @param other The geometry to check
    * @param dist The radius of the circle within which this check is conducted
    */
  def withinDistance(other: Geometry, dist: Double): Boolean =
    jtsGeom.isWithinDistance(other.jtsGeom, dist)

  /** Calculate centroid of this Geometry */
  def centroid: PointOrNoResult =
    jtsGeom.getCentroid

  def interiorPoint: PointOrNoResult =
    jtsGeom.getInteriorPoint

  def envelope: Extent =
    if(jtsGeom.isEmpty) Extent(0.0, 0.0, 0.0, 0.0)
    else jtsGeom.getEnvelopeInternal

  /** Get the number of vertices in this geometry */
  lazy val vertexCount: Int = jtsGeom.getNumPoints

  /** Returns this Geometry's vertices. */
  lazy val vertices: Array[Point] = {
    val vertices = for (i <- 0 until jtsGeom.getNumGeometries) yield {
      Geometry(jtsGeom.getGeometryN(i).clone().asInstanceOf[jts.Geometry]).vertices
    }

    vertices.reduce(_ ++ _)
  }


  def &(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    intersection(g)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this Polygon and g.
    */
  def intersection(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of the
    * points shared by this Polygon and g. If it fails, it reduces the
    * precision to avoid TopologyException.
    */
  def intersectionSafe(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    try intersection(g)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(g.jtsGeom))
    }

  def intersects(other: Geometry): Boolean =
    jtsGeom.intersects(other.jtsGeom)

  def disjoint(other: Geometry): Boolean =
    jtsGeom.disjoint(other.jtsGeom)

  /** Attempt to convert this Geometry to the provided type */
  def as[G <: Geometry : ClassTag]: Option[G] = {
    if (classTag[G].runtimeClass.isInstance(this))
      Some(this.asInstanceOf[G])
    else
      None
  }

  protected def populatePoints(sequence: CoordinateSequence, arr: Array[Point], offset: Int = 0): Array[Point] = {
    cfor(0)(_ < sequence.size, _ + 1) { i =>
      arr(i + offset) = Point(sequence.getX(i), sequence.getY(i))
    }

    arr
  }

  override
  def equals(other: Any): Boolean =
    other match {
      case g: Geometry => jtsGeom.equals(g.jtsGeom)
      case _ => false
  }

  override
  def hashCode(): Int = jtsGeom.hashCode

  override def toString = jtsGeom.toString
}

/** Geometry companion object */
object Geometry {

  /** Wraps JTS Geometry in correct container. Useful when sourcing objects from JTS interface. */
  implicit def apply(obj: jts.Geometry): Geometry =
    obj match {
      case obj: jts.Point => Point(obj)
      case obj: jts.LineString => Line(obj)
      case obj: jts.Polygon => Polygon(obj)
      case obj: jts.MultiPoint => MultiPoint(obj)
      case obj: jts.MultiLineString => MultiLine(obj)
      case obj: jts.MultiPolygon => MultiPolygon(obj)
      case obj: jts.GeometryCollection => GeometryCollection(obj)
    }
}
