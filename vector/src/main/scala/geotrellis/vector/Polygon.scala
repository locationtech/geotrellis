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

import org.locationtech.jts.geom.{CoordinateSequence, LinearRing, TopologyException}
import org.locationtech.jts.operation.union._
import org.locationtech.jts.{geom => jts}
import geotrellis.vector.GeomFactory._

import scala.collection.GenTraversable
import scala.collection.JavaConversions._

object Polygon {
  implicit def jtsToPolygon(jtsGeom: jts.Polygon): Polygon =
    Polygon(jtsGeom)

  def apply(exterior: Point*)(implicit d: DummyImplicit): Polygon =
    apply(Line(exterior), Set())

  def apply(exterior: Seq[Point]): Polygon =
    apply(Line(exterior), Set())

  def apply(exterior: Line): Polygon =
    apply(exterior, Set())

  def apply(exterior: CoordinateSequence): Polygon =
    Polygon(factory.createPolygon(exterior))

  def apply(exterior: Line, holes:Line*): Polygon =
    apply(exterior, holes)

  def apply(exterior: CoordinateSequence, holes: GenTraversable[CoordinateSequence]): Polygon =
    apply(factory.createLinearRing(exterior), holes.map(factory.createLinearRing))

  def apply(exterior: Line, holes: GenTraversable[Line]): Polygon = {
    if(!exterior.isClosed) {
      sys.error(s"Cannot create a polygon with unclosed exterior: $exterior")
    }

    if(exterior.vertexCount < 4) {
      sys.error(s"Cannot create a polygon with exterior with fewer than 4 points: $exterior")
    }

    val extGeom = factory.createLinearRing(exterior.jtsGeom.getCoordinateSequence)

    val holeGeoms = (
      for (hole <- holes) yield {
        if (!hole.isClosed) {
          sys.error(s"Cannot create a polygon with an unclosed hole: $hole")
        } else {
          if (hole.vertexCount < 4) {
            sys.error(s"Cannot create a polygon with a hole with fewer than 4 points: $hole")
          } else {
            factory.createLinearRing(hole.jtsGeom.getCoordinateSequence)
          }
        }
      }).toArray

    apply(extGeom, holeGeoms)
  }

  def apply(exterior: LinearRing, holes: GenTraversable[LinearRing]): Polygon =
    Polygon(factory.createPolygon(exterior, holes.toArray))
}

/** Class representing a polygon */
case class Polygon(jtsGeom: jts.Polygon) extends Geometry
                                            with TwoDimensions {

  assert(!jtsGeom.isEmpty, s"Polygon Empty: $jtsGeom")

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): Polygon = {
    val geom = jtsGeom.clone.asInstanceOf[jts.Polygon]
    geom.normalize
    Polygon(geom)
  }

  /** Tests whether this Polygon is a rectangle. */
  lazy val isRectangle: Boolean =
    jtsGeom.isRectangle

  /** Returns the area of this Polygon. */
  lazy val area: Double =
    jtsGeom.getArea

  /** Returns the exterior ring of this Polygon. */
  lazy val exterior: Line =
    Line(jtsGeom.getExteriorRing.clone.asInstanceOf[jts.LineString])

  /** Returns the hole rings of this Polygon. */
  lazy val holes: Array[Line] = {
    for (i <- 0 until numberOfHoles) yield
      Line(jtsGeom.getInteriorRingN(i).clone.asInstanceOf[jts.LineString])
  }.toArray

  /** Returns true if this Polygon contains holes */
  lazy val hasHoles: Boolean =
    numberOfHoles > 0

  /** Returns the number of holes in this Polygon */
  lazy val numberOfHoles: Int =
    jtsGeom.getNumInteriorRing

  /**
   * Returns the boundary of this Polygon.
   * The boundary of a Polygon is the set of closed curves corresponding to its
   * exterior and interior boundaries.
   */
  lazy val boundary: PolygonBoundaryResult =
    jtsGeom.getBoundary

  /** Returns this Polygon's vertices. */
  override lazy val vertices: Array[Point] = {
    val arr = Array.ofDim[Point](jtsGeom.getNumPoints)

    val sequences = jtsGeom.getExteriorRing.getCoordinateSequence +: (0 until jtsGeom.getNumInteriorRing).map(jtsGeom
      .getInteriorRingN(_).getCoordinateSequence)
    val offsets = sequences.map(_.size).scanLeft(0)(_ + _).dropRight(1)

    sequences.zip(offsets).foreach { case (seq, offset) => populatePoints(seq, arr, offset) }

    arr
  }

  /**
   * Returns this Polygon's perimeter.
   * A Polygon's perimeter is the length of its exterior and interior
   * boundaries.
   */
  lazy val perimeter: Double =
    jtsGeom.getLength

  // -- Intersection

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and p.
   */
  def &(p: Point): PointOrNoResult =
    intersection(p)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and p.
   */
  def intersection(p: Point): PointOrNoResult =
    jtsGeom.intersection(p.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of the
    * points shared by this Polygon and g. If it fails, it reduces the
    * precision to avoid TopologyException.
    */
  def intersectionSafe(p: Point): PointOrNoResult =
    try intersection(p)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(p.jtsGeom))
    }

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and mp.
   */
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    intersection(mp)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and mp.
   */
  def intersection(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of the
    * points shared by this Polygon and g. If it fails, it reduces the
    * precision to avoid TopologyException.
    */
  def intersectionSafe(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    try intersection(mp)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(mp.jtsGeom))
    }

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def &(g: OneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(g)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def intersection(g: OneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of the
    * points shared by this Polygon and g. If it fails, it reduces the
    * precision to avoid TopologyException.
    */
  def intersectionSafe(g: OneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    try intersection(g)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(g.jtsGeom))
    }

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def &(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    intersection(g)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def intersection(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of the
    * points shared by this Polygon and g. If it fails, it reduces the
    * precision to avoid TopologyException.
    */
  def intersectionSafe(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    try intersection(g)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(g.jtsGeom))
    }

  // -- Union

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g.
   */
  def |(g: AtMostOneDimension): AtMostOneDimensionPolygonUnionResult =
    union(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g.
   */
  def union(g: AtMostOneDimension): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g.
   */
  def |(g: TwoDimensions): TwoDimensionsTwoDimensionsUnionResult =
    union(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g. Uses cascaded polygon union if g is a (multi)polygon
   * else falls back to default jts union method.
   */
  def union(g: TwoDimensions): TwoDimensionsTwoDimensionsUnionResult = g match {
    case p:Polygon =>
      new CascadedPolygonUnion(Seq(this, p).map(_.jtsGeom)).union
    case mp:MultiPolygon =>
      new CascadedPolygonUnion((this +: mp.polygons).map(_.jtsGeom).toSeq).union
    case _ =>
      jtsGeom.union(g.jtsGeom)
  }



  // -- Difference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def -(g: AtMostOneDimension): PolygonAtMostOneDimensionDifferenceResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def difference(g: AtMostOneDimension): PolygonAtMostOneDimensionDifferenceResult =
    jtsGeom.difference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def -(g: TwoDimensions): TwoDimensionsTwoDimensionsDifferenceResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def difference(g: TwoDimensions): TwoDimensionsTwoDimensionsDifferenceResult =
    jtsGeom.difference(g.jtsGeom)


  // -- SymDifference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g and all the points in g that are not in
   * this Polygon.
   */
  def symDifference(g: AtMostOneDimension): AtMostOneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g and all the points in g that are not in
   * this Polygon.
   */
  def symDifference(g: TwoDimensions): TwoDimensionsTwoDimensionsSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)


  // -- Buffer


  /** Computes a buffer area around this Polygon having width d. */
  def buffer(d: Double): Polygon =
    jtsGeom.buffer(d) match {
      case p: jts.Polygon => Polygon(p)
      case x =>
        sys.error(s"Unexpected result for Polygon buffer: ${x.getGeometryType}")
    }

  // -- Predicates


  /**
   * Tests whether this Polygon contains the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF*.
   */
  def contains(g: Geometry): Boolean =
    jtsGeom.contains(g.jtsGeom)

  /**
   * Tests whether this Polygon is covered by the specified TwoDimensions g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is T*F**F*** or
   * *TF**F*** or **FT*F*** or **F*TF***.
   */
  def coveredBy(g: TwoDimensions): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  /**
   * Tests whether this Polygon covers the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF* or *T****FF* or ***T**FF* or ****T*FF*.
   */
  def covers(g: Geometry): Boolean =
    jtsGeom.covers(g.jtsGeom)

  /**
   * Tests whether this Polygon crosses the specified MultiPoint mp.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****T** (A/P).
   */
  def crosses(mp: MultiPoint): Boolean =
    jtsGeom.crosses(mp.jtsGeom)

  /**
   * Tests whether this Polygon crosses the specified OneDimension g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****T** (A/L).
   */
  def crosses(g: OneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /**
   * Tests whether this Polygon overlaps the specified TwoDimensions g.
   * Returns true if The DE-9IM Intersection Matrix for the two geometries is
   * T*T***T**.
   */
  def overlaps(g: TwoDimensions): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  /**
   * Tests whether this Polygon touches the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * FT*******, F**T***** or F***T****.
   */
  def touches(g: Geometry): Boolean =
    jtsGeom.touches(g.jtsGeom)

  /**
   * Tests whether this Polygon is within the specified TwoDimensions g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*F**F***.
   */
  def within(g: TwoDimensions): Boolean =
    jtsGeom.within(g.jtsGeom)
}
