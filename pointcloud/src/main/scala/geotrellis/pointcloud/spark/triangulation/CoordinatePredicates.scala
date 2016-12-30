package geotrellis.pointcloud.spark.triangulation

import geotrellis.vector.triangulation._
import geotrellis.pointcloud.spark._
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import org.apache.commons.math3.linear._
import com.vividsolutions.jts.geom.Coordinate

object CoordinatePredicates {
  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    new LUDecomposition(m).getDeterminant
  }

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

  def circleCenter(a: Coordinate, b: Coordinate, c: Coordinate): Coordinate = {
    val d = 2.0 * det3(a.x, a.y, 1.0,
                       b.x, b.y, 1.0,
                       c.x, c.y, 1.0)
    val h = det3(a.x * a.x + a.y * a.y, a.y, 1.0,
                 b.x * b.x + b.y * b.y, b.y, 1.0,
                 c.x * c.x + c.y * c.y, c.y, 1.0) / d
    val k = det3(a.x, a.x * a.x + a.y * a.y, 1.0,
                 b.x, b.x * b.x + b.y * b.y, 1.0,
                 c.x, c.x * c.x + c.y * c.y, 1.0) / d
    new Coordinate(h,k)
  }

  def circleCenter[V](ai: V, bi: V, ci: V)(implicit trans: V => Coordinate): Coordinate = {
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    val d = 2.0 * det3(a.x, a.y, 1.0,
                       b.x, b.y, 1.0,
                       c.x, c.y, 1.0)
    val h = det3(a.x * a.x + a.y * a.y, a.y, 1.0,
                 b.x * b.x + b.y * b.y, b.y, 1.0,
                 c.x * c.x + c.y * c.y, c.y, 1.0) / d
    val k = det3(a.x, a.x * a.x + a.y * a.y, 1.0,
                 b.x, b.x * b.x + b.y * b.y, 1.0,
                 c.x, c.x * c.x + c.y * c.y, 1.0) / d
    new Coordinate(h,k)
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
