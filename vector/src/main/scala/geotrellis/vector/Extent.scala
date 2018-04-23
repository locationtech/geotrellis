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

import GeomFactory._
import geotrellis.proj4.{CRS, Transform}

import org.locationtech.jts.{geom => jts}

case class ExtentRangeError(msg:String) extends Exception(msg)

object Extent {
  def apply(env: jts.Envelope): Extent =
    Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

  /** Create an extent from a string
    *
    * @param s   A string of the form "xmin,ymin,xmax,ymax"
    */
  def fromString(s:String) = {
    val Array(xmin,ymin,xmax,ymax) = s.split(",").map(_.toDouble)
    Extent(xmin,ymin,xmax,ymax)
  }

  implicit def toPolygon(extent: Extent): Polygon =
    extent.toPolygon

  implicit def jts2Extent(env: jts.Envelope): Extent =
    Extent(env)
}

/** A case class for an extent and its corresponding CRS
  *
  * @param extent The Extent which is projected
  * @param crs    The CRS projection of this extent
  */
case class ProjectedExtent(extent: Extent, crs: CRS) {
  def reproject(dest: CRS): Extent =
    extent.reproject(crs, dest)

  /** Performs adaptive refinement to produce a Polygon representation of the projected region.
    *
    * Generally, rectangular regions become curvilinear regions after geodetic projection.
    * This function creates a polygon giving an accurate representation of the post-projection
    * region.  This function does its work by recursively splitting extent edges, until the
    * subdivided edge is "close enough" to the unrefined edge.
    *
    * @param dest
    * @param relError   A tolerance value telling how much deflection is allowed in terms of
    *                   distance from the original line to the new point
    */
  def reprojectAsPolygon(dest: CRS, relError: Double = 0.01): Polygon = {
    val transform = Transform(crs, dest)

    def refine(p0: (Point, (Double, Double)), p1: (Point, (Double, Double))): List[(Point, (Double, Double))] = {
      val ((a, (x0, y0)), (b, (x1, y1))) = (p0, p1)
      val m = Point(0.5 * (a.x + b.x), 0.5 * (a.y + b.y))
      val (x2, y2) = transform(m.x, m.y)

      import math.{abs, pow, sqrt}

      val deflect = abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1) / sqrt(pow(y2 - y1, 2) + pow(x2 - x1, 2))
      val length = sqrt(pow(x0 - x1, 2) + pow(y0 - y1, 2))

      val p2 = m -> (x2, y2)
      if (deflect / length < relError) {
        List(p2)
      } else {
        refine(p0, p2) ++ (p2 :: refine(p2, p1))
      }
    }

    val pts = Array(extent.southWest, extent.southEast, extent.northEast, extent.northWest)
      .map{ p => (p, transform(p.x, p.y)) }

    Polygon ( ((pts(0) :: refine(pts(0), pts(1))) ++ 
               (pts(1) :: refine(pts(1), pts(2))) ++ 
               (pts(2) :: refine(pts(2), pts(3))) ++
               (pts(3) :: refine(pts(3), pts(0))) ++
               List(pts(0))).map{ case (_, (x, y)) => Point(x, y) } )
  }
}

/** ProjectedExtent companion object */
object ProjectedExtent {
  implicit def fromTupleA(tup: (Extent, CRS)):ProjectedExtent = ProjectedExtent(tup._1, tup._2)
  implicit def fromTupleB(tup: (CRS, Extent)):ProjectedExtent = ProjectedExtent(tup._2, tup._1)
}

/** A rectangular region of geographic space
  *
  * @param xmin The minimum x coordinate
  * @param ymin The minimum y coordinate
  * @param xmax The maximum x coordinate
  * @param ymax The maximum y coordinate
  */
case class Extent(
  xmin: Double, ymin: Double,
  xmax: Double, ymax: Double
) {

  // Validation: Do not accept extents min values greater than max values.
  if (xmin > xmax) { throw ExtentRangeError(s"Invalid Extent: xmin must be less than xmax (xmin=$xmin, xmax=$xmax)") }
  if (ymin > ymax) { throw ExtentRangeError(s"Invalid Extent: ymin must be less than ymax (ymin=$ymin, ymax=$ymax)") }

  def jtsEnvelope: org.locationtech.jts.geom.Envelope =
    new org.locationtech.jts.geom.Envelope(xmin, xmax, ymin, ymax)

  val width: Double = xmax - xmin
  val height: Double = ymax - ymin

  def min: Point = Point(xmin, ymin)
  def max: Point = Point(xmax, ymax)

  /** The SW corner (xmin, ymin) as a Point. */
  def southWest: Point = Point(xmin, ymin)

  /** The SE corner (xmax, ymin) as a Point. */
  def southEast: Point = Point(xmax, ymin)

  /** The NE corner (xmax, ymax) as a Point. */
  def northEast: Point = Point(xmax, ymax)

  /** The NW corner (xmin, ymax) as a Point. */
  def northWest: Point = Point(xmin, ymax)

  /** The area of this extent */
  def area: Double = width * height

  /** The minimum between the height and width of this extent */
  def minExtent: Double = if(width < height) width else height

  /** The maximum between the height and width of this extent */
  def maxExtent: Double = if(width > height) width else height

  /** Predicate for whether this extent has 0 area */
  def isEmpty: Boolean = area == 0

  /** The centroid of this extent */
  def center: Point =
    Point((xmin + xmax) / 2.0, (ymin + ymax) / 2.0)

  /** Predicate for whether this extent intersects the interior of another */
  def interiorIntersects(other: Extent): Boolean =
    !(other.xmax <= xmin ||
      other.xmin >= xmax) &&
  !(other.ymax <= ymin ||
    other.ymin >= ymax)

  /** Predicate for whether this extent intersects another */
  def intersects(other: Extent): Boolean =
    !(other.xmax < xmin ||
      other.xmin > xmax) &&
  !(other.ymax < ymin ||
    other.ymin > ymax)

  /** Predicate for whether this extent intersects another */
  def intersects(p: Point): Boolean =
    intersects(p.x, p.y)

  /** Predicate for whether this extent intersects the specified point */
  def intersects(x: Double, y: Double): Boolean =
    x >= xmin && x <= xmax && y >= ymin && y <= ymax

  /** Empty extent contains nothing, though non empty extent contains iteslf */
  def contains(other: Extent): Boolean = {
    if(xmin == 0 && xmax == 0 && ymin == 0 && ymax == 0) false
    else
      other.xmin >= xmin &&
    other.ymin >= ymin &&
    other.xmax <= xmax &&
    other.ymax <= ymax
  }

  /** Tests if the given point lies in or on the envelope.
    *
    * @note This is the same definition as the SFS <tt>contains</tt>,
    *       which is unlike the JTS Envelope.contains, which would include the
    *       envelope boundary.
    */
  def contains(p: Point): Boolean =
    contains(p.x, p.y)

  /** Tests if the given point lies in or on the envelope.
    *
    * @note This is the same definition as the SFS <tt>contains</tt>,
    *       which is unlike the JTS Envelope.contains, which would include the
    *       envelope boundary.
    */
  def contains(x: Double, y: Double): Boolean =
    x > xmin && x < xmax && y > ymin && y < ymax

  /** Predicate for whether this extent covers another */
  def covers(other: Extent): Boolean =
    contains(other)

  /** Predicate for whether this extent covers a point */
  def covers(p: Point): Boolean =
    covers(p.x, p.y)

  /** Predicate for whether this extent covers a point */
  def covers(x: Double, y: Double): Boolean =
    intersects(x, y)

  /** Distance from another extent */
  def distance(other: Extent): Double =
    if(intersects(other)) 0
    else {
      val dx =
        if(xmax < other.xmin)
          other.xmin - xmax
        else if(xmin > other.xmax)
          xmin - other.xmax
        else
          0.0

      val dy =
        if(ymax < other.ymin)
          other.ymin - ymax
        else if(ymin > other.ymax)
          ymin - other.ymax
        else
          0.0

      // if either is zero, the envelopes overlap either vertically or horizontally
      if(dx == 0.0)
        dy
      else if(dy == 0.0)
        dx
      else
        math.sqrt(dx * dx + dy * dy)
    }

  /** Create an optional extent which represents the intersection with a provided extent */
  def intersection(other: Extent): Option[Extent] = {
    val xminNew = if(xmin > other.xmin) xmin else other.xmin
    val yminNew = if(ymin > other.ymin) ymin else other.ymin
    val xmaxNew = if(xmax < other.xmax) xmax else other.xmax
    val ymaxNew = if(ymax < other.ymax) ymax else other.ymax

    if(xminNew <= xmaxNew && yminNew <= ymaxNew) {
      Some(Extent(xminNew, yminNew, xmaxNew, ymaxNew))
    } else { None }
  }

  /** Create an optional extent which represents the intersection with a provided extent */
  def &(other: Extent): Option[Extent] =
    intersection(other)

  /** Create a new extent using a buffer around this extent */
  def buffer(d: Double): Extent =
    Extent(xmin - d, ymin - d, xmax + d, ymax + d)

  /** Orders two bounding boxes by their (geographically) lower-left corner. The bounding box
    * that is further south (or west in the case of a tie) comes first.
    *
    * If the lower-left corners are the same, the upper-right corners are
    * compared. This is mostly to assure that 0 is only returned when the
    * extents are equal.
    *
    * Return type signals:
    *
    *   -1 this bounding box comes first
    *    0 the bounding boxes have the same lower-left corner
    *    1 the other bounding box comes first
    */
  def compare(other: Extent): Int = {
    var cmp = ymin compare other.ymin
    if (cmp != 0) return cmp

    cmp = xmin compare other.xmin
    if (cmp != 0) return cmp

    cmp = ymax compare other.ymax
    if (cmp != 0) return cmp

    xmax compare other.xmax
  }

  /** Return the smallest extent that contains this extent and the provided extent. */
  def combine(other:Extent): Extent =
    Extent(
      if(xmin < other.xmin) xmin else other.xmin,
      if(ymin < other.ymin) ymin else other.ymin,
      if(xmax > other.xmax) xmax else other.xmax,
      if(ymax > other.ymax) ymax else other.ymax
    )

  /** Return the smallest extent that contains this extent and the provided extent. */
  def expandToInclude(other: Extent): Extent =
    combine(other)

  /** Return the smallest extent that contains this extent and the provided point. */
  def expandToInclude(p: Point): Extent =
    expandToInclude(p.x, p.y)

  /** Return the smallest extent that contains this extent and the provided point. */
  def expandToInclude(x: Double, y: Double): Extent =
    Extent(
      if(xmin < x) xmin else x,
      if(ymin < y) ymin else y,
      if(xmax > x) xmax else x,
      if(ymax > y) ymax else y
    )

  /** Return an extent of this extent expanded by the provided distance on all sides */
  def expandBy(distance: Double): Extent =
    expandBy(distance, distance)


  /** Return an extent of this extent expanded by the provided x and y distances */
  def expandBy(deltaX: Double, deltaY: Double): Extent =
    Extent(
      xmin - deltaX,
      ymin - deltaY,
      xmax + deltaX,
      ymax + deltaY
    )

  /** Return this extent moved x and y amounts */
  def translate(deltaX: Double, deltaY: Double): Extent =
    Extent(
      xmin + deltaX,
      ymin + deltaY,
      xmax + deltaX,
      ymin + deltaY
    )

  /** Return this extent as a polygon */
  def toPolygon(): Polygon =
    Polygon( Line((xmin, ymin), (xmin, ymax), (xmax, ymax), (xmax, ymin), (xmin, ymin)) )

  /** Equality check against this extent
    *
    * @note only returns true given another extent
    */
  override
  def equals(o: Any): Boolean =
    o match {
      case other: Extent =>
        xmin == other.xmin && ymin == other.ymin &&
        xmax == other.xmax && ymax == other.ymax
      case _ => false
    }

  override
  def hashCode(): Int = (xmin, ymin, xmax, ymax).hashCode

  override def toString = s"Extent($xmin, $ymin, $xmax, $ymax)"
}
