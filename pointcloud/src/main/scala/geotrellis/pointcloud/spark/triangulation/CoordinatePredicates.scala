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

package geotrellis.pointcloud.spark.triangulation

import geotrellis.vector.{RobustPredicates, ShewchuksDeterminant}
import geotrellis.vector.triangulation._
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}

import org.apache.commons.math3.linear._
import com.vividsolutions.jts.geom.Coordinate

object CoordinatePredicates {
  def isCollinear(a: Coordinate, b: Coordinate, c: Coordinate): Boolean =
    math.abs(ShewchuksDeterminant.orient2d(a.x, a.y, b.x, b.y, c.x, c.y)) < EPSILON

  def isCollinear[V,T](e: HalfEdge[V, T], v: V)(implicit trans: V => Coordinate): Boolean = {
    val a = trans(e.src)
    val b = trans(e.vert)
    val c = trans(v)

    isCollinear(a, b, c)
  }

  def doesExtend[V,T](e: HalfEdge[V, T], v: V)(implicit trans: V => Coordinate): Boolean = {
    val a = trans(e.src)
    val b = trans(e.vert)
    val c = trans(v)

    val d1 = math.pow(a.x - b.x, 2) + math.pow(a.y - b.y, 2)
    val d2 = math.pow(a.x - c.x, 2) + math.pow(a.y - c.y, 2)

    d1 < d2
  }

  def isCorner[V,T](edge: HalfEdge[V,T])(implicit trans: V => Coordinate): Boolean = {
    !isCollinear(edge, edge.prev.src) || {
      val c = trans(edge.src)
      val n = trans(edge.vert)
      val p = trans(edge.prev.src)
      val (xn, yn) = (n.x - c.x, n.y - c.y)
      val (xp, yp) = (p.x - c.x, p.y - c.y)
      xn * xp + yn * yp > 0
    }
  }

  def isCCW(a: Coordinate, b: Coordinate, c: Coordinate): Boolean = {
    // det [ a.x-c.x  a.y-c.y ]
    //     [ b.x-c.x  b.y-c.y ] > 0
    ShewchuksDeterminant.orient2d(a.x, a.y, b.x, b.y, c.x, c.y) > EPSILON
  }

  def isRightOf[V,T](e: HalfEdge[V,T], p: Coordinate)(implicit trans: V => Coordinate) =
    isCCW(p, trans(e.vert), trans(e.src))

  def isRightOf[V,T](e: HalfEdge[V,T], p: V)(implicit trans: V => Coordinate) =
    isCCW(trans(p), trans(e.vert), trans(e.src))

  def isLeftOf[V,T](e: HalfEdge[V,T], p: Coordinate)(implicit trans: V => Coordinate) =
    isCCW(p, trans(e.src), trans(e.vert))

  def isLeftOf[V,T](e: HalfEdge[V,T], p: V)(implicit trans: V => Coordinate) =
    isCCW(trans(p), trans(e.src), trans(e.vert))

  def inCircle(abc: (Coordinate, Coordinate, Coordinate), d: Coordinate): Boolean = {
    val (a,b,c) = abc
    // det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
    //      b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
    //      c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > EPSILON
    ShewchuksDeterminant.incircle(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y) > EPSILON
  }

  def inCircle[V](abc: (V, V, V), di: V)(implicit trans: V => Coordinate): Boolean = {
    val (ai,bi,ci) = abc
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    val d = trans(di)
    ShewchuksDeterminant.incircle(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y) > EPSILON
  }

  def circleCenter[V](ai: V, bi: V, ci: V)(implicit trans: V => Coordinate): (Double, Coordinate) = {
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)

    RobustPredicates.circleCenter(a.x, a.y, b.x, b.y, c.x, c.y)
  }

  def isDelaunayEdge[V,T](e: HalfEdge[V,T])(implicit trans: V => Coordinate): Boolean = {
    // Predicated on the fact that if an edge is Delaunay, then for a
    // point, A, to the left of edge (X,Y), and a point, B, to the
    // right of (X,Y), A may not be in the circle defined by points X,
    // Y, and B.
    val a = trans(e.next.vert)
    val b = trans(e.flip.next.vert)
    val x = trans(e.flip.vert)
    val y = trans(e.vert)
    !inCircle((a, x, y), b)
  }
}
