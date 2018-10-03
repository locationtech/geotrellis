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

package geotrellis.vector.triangulation

import org.locationtech.jts.geom.Coordinate
import org.apache.commons.math3.linear._
import geotrellis.vector.ShewchuksDeterminant
import geotrellis.vector.RobustPredicates
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector.mesh.{HalfEdgeTable, IndexedPointSet}


final class TriangulationPredicates(
  pointSet: IndexedPointSet,
  halfEdgeTable: HalfEdgeTable
) extends Serializable {
  import pointSet._
  import halfEdgeTable._

  def isCollinear(a: Int, b: Int, c: Int): Boolean =
    math.abs(
      ShewchuksDeterminant.orient2d(
        getX(a), getY(a),
        getX(b), getY(b),
        getX(c), getY(c)
      )
    ) < EPSILON

  def isCollinear(e: Int, v: Int): Boolean =
    isCollinear(getSrc(e), getDest(e), v)

  def doesExtend(e: Int, v: Int): Boolean = {
    val a = getSrc(e)
    val ax = getX(a)
    val ay = getY(a)
    val b = getDest(e)
    val bx = getX(b)
    val by = getY(b)
    val cx = getX(v)
    val cy = getY(v)

    val d1 = math.pow(ax - bx, 2) + math.pow(ay - by, 2)
    val d2 = math.pow(ax - cx, 2) + math.pow(ay - cy, 2)

    d1 < d2
  }

  def isCorner(edge: Int): Boolean = {
    !isCollinear(edge, getSrc(getPrev(edge))) || {
      val c = getSrc(edge)
      val cx = getX(c)
      val cy = getY(c)
      val n = getDest(edge)
      val nx = getX(n)
      val ny = getY(n)
      val p = getSrc(getPrev(edge))
      val px = getX(p)
      val py = getY(p)
      val (xn, yn) = (nx - cx, ny - cy)
      val (xp, yp) = (px - cx, py - cy)
      xn * xp + yn * yp > 0
    }
  }

  def relativeTo(e: Int, p: Int): Int = {
    val e0 = getSrc(e)
    val e0x = getX(e0)
    val e0y = getY(e0)
    val e1 = getDest(e)
    val e1x = getX(e1)
    val e1y = getY(e1)
    val ptx = getX(p)
    val pty = getY(p)

    RobustPredicates.relativeTo(e0x, e0y, e1x, e1y, ptx, pty)
  }

  def isCCW(a: Int, b: Int, c: Int) = {
    val ax = getX(a)
    val ay = getY(a)
    val bx = getX(b)
    val by = getY(b)
    val cx = getX(c)
    val cy = getY(c)
    RobustPredicates.isCCW(ax, ay, bx, by, cx, cy)
  }

  def isRightOf(e: Int, p: Int) = {
    val px = getX(p)
    val py = getY(p)
    val b = getDest(e)
    val bx = getX(b)
    val by = getY(b)
    val c = getSrc(e)
    val cx = getX(c)
    val cy = getY(c)
    RobustPredicates.isCCW(px, py, bx, by, cx, cy)
  }

  def isLeftOf(e: Int, p: Int) = {
    val px = getX(p)
    val py = getY(p)
    val b = getSrc(e)
    val bx = getX(b)
    val by = getY(b)
    val c = getDest(e)
    val cx = getX(c)
    val cy = getY(c)
    RobustPredicates.isCCW(px, py, bx, by, cx, cy)
  }

  def inCircle(ai: Int, bi: Int, ci: Int, di: Int): Boolean = {
    val ax = getX(ai)
    val ay = getY(ai)
    val bx = getX(bi)
    val by = getY(bi)
    val cx = getX(ci)
    val cy = getY(ci)
    val dx = getX(di)
    val dy = getY(di)
    RobustPredicates.inCircle(ax, ay, bx, by, cx, cy, dx, dy)
  }

  def circleCenter(ai: Int, bi: Int, ci: Int): (Double, Coordinate, Boolean) = {
    val ax = getX(ai)
    val ay = getY(ai)
    val bx = getX(bi)
    val by = getY(bi)
    val cx = getX(ci)
    val cy = getY(ci)
    RobustPredicates.circleCenter(ax, ay, bx, by, cx, cy)
  }

  def isDelaunayEdge(e: Int): Boolean = {
    // Predicated on the fact that if an edge is Delaunay, then for a
    // point, A, to the left of edge (X,Y), and a point, B, to the
    // right of (X,Y), A may not be in the circle defined by points X,
    // Y, and B.
    val a = getDest(getNext(e))
    val b = getDest(getNext(getFlip(e)))
    val x = getDest(getFlip(e))
    val y = getDest(e)
    !inCircle(a, x, y, b)
  }

  def isConvexBoundary(e0: Int): Boolean = {
    var e = e0
    var valid = true
    do {
      valid = valid && !isLeftOf(e, getDest(getNext(e)))
      e = getNext(e)
    } while (valid && e != e0)
    valid
  }
}
