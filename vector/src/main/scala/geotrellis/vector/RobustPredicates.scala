/*
 * Copyright 2018 Azavea
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

import org.locationtech.jts.geom.Coordinate
import org.apache.commons.math3.linear._

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}

/** Provides a set of numerically-sound geometric predicates.
 */
object RobustPredicates {
  final val LEFTOF = -1
  final val RIGHTOF = 1
  final val ON = 0

  def det2 (a11: Double, a12: Double,
            a21: Double, a22: Double): Double = {
    a11 * a22 - a12 * a21
  }

  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    (new LUDecomposition(m)).getDeterminant
  }

  /**
   * Given three points in 2d, represented as the Double pairs `(ax, ay)`,
   * `(bx, by)`, and `(cx, cy)`, this function returns true if all three points
   * lie on a single line, up to the limits of numerical precision.
   */
  def isCollinear(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double
  ): Boolean = {
    math.abs(ShewchuksDeterminant.orient2d(ax, ay, bx, by, cx, cy)) < EPSILON
  }

  /**
   * Given three points in 2d, represented as jts Coordinates, this function
   * returns true if all three points lie on a single line, up to the limits of
   * numerical precision.  The z-coordinates of the input points are ignored.
   */
  def isCollinear(a: Coordinate, b: Coordinate, c: Coordinate): Boolean = {
    isCollinear(a.x, a.y, b.x, b.y, c.x, c.y)
  }

  /**
   * Given three points in 2d, represented as the Double pairs `(ax, ay)`,
   * `(bx, by)`, and `(cx, cy)`, this function returns true if the three points,
   * in the order `a`, `b`, and then `c`, have a counterclockwise winding.  This
   * function returns false if the winding of the points is clockwise, or if the
   * points are collinear.
   */
  def isCCW(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double
  ): Boolean = {
    ShewchuksDeterminant.orient2d(ax, ay, bx, by, cx, cy) > EPSILON
  }

  /**
   * Given three points in 2d, represented as jts Coordinates, this function
   * returns true if the three points, visted in the order `a`, `b`, and then
   * `c`, have a counterclockwise winding.  This function returns false if the
   * winding of the points is clockwise, or if the points are collinear.  The
   * z-coordinates of the input points are ignored.
   */
  def isCCW(a: Coordinate, b: Coordinate, c: Coordinate): Boolean =
    isCCW(a.x, a.y, b.x, b.y, c.x, c.y)

  /**
   * Given four points in 2d, represented as the Double pairs `(ax, ay)`,
   * `(bx, by)`, `(cx, cy)`, and `(dx, dy)`, this function determines if the
   * unique circle having points `a`, `b`, and `c` on its boundary
   * (`isCCW(a, b, c)` must be true) contains point `d` in its interior.
   */
  def relativeTo(e0x: Double, e0y: Double, e1x: Double, e1y: Double, px: Double, py: Double): Int = {
    val det = ShewchuksDeterminant.orient2d(e0x, e0y, e1x, e1y, px, py)

    if(det > EPSILON)
      LEFTOF
    else if(det < -EPSILON)
      RIGHTOF
    else
      ON
  }

  def relativeTo(e0: Coordinate, e1: Coordinate, p: Coordinate): Int = {
    relativeTo(e0.x, e0.y, e1.x, e1.y, p.x, p.y)
  }

  def inCircle(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double,
    dx: Double, dy: Double
  ): Boolean = {
    ShewchuksDeterminant.incircle(ax, ay, bx, by, cx, cy, dx, dy) > EPSILON
  }

  /**
   * Given four points in 2d, represented as jts Coordinates, this function
   * determines if the unique circle having points `a`, `b`, and `c` on its
   * boundary (`isCCW(a, b, c)` must be true) contains point `d` in its
   * interior.  The z-coordinates of the input points are ignored.
   */
  def inCircle(a: Coordinate, b: Coordinate, c: Coordinate, d: Coordinate): Boolean =
    inCircle(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y)

  /**
   * Given three points in 2d, represented as the Double pairs `(ax, ay)`,
   * `(bx, by)`, and `(cx, cy)`, this function finds the center of the unique
   * circle that contains the three points on its boundary.  The return of this
   * function is a triple containing the radius of the circle, the center of the
   * circle, represented as a jts Coordinate, and a Boolean flag indicating if
   * the radius and center are numerically reliable.  This last value will be
   * false if the points are too close to being collinear.
   */
  def circleCenter(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): (Double, Coordinate, Boolean) = {
    val dbx = bx - ax
    val dby = by - ay
    val dcx = cx - ax
    val dcy = cy - ay

    val lenΔb2 = dbx * dbx + dby * dby
    val lenΔc2 = dcx * dcx + dcy * dcy

    val d = 2.0 * det2(dbx, dby,
                       dcx, dcy)
    val h = ax - det2(dby, lenΔb2,
                      dcy, lenΔc2) / d
    val k = ay + det2(dbx, lenΔb2,
                      dcx, lenΔc2) / d

    val r = math.sqrt(lenΔb2 * lenΔc2 * ((bx - cx) * (bx - cx) + (by - cy) * (by - cy))) / d

    (r, new Coordinate(h,k), d > 2e-8)
  }

}
