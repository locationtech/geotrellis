package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate
import org.apache.commons.math3.linear._

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}

object Predicates {
  final val LEFTOF = -1
  final val RIGHTOF = 1
  final val ON = 0

  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    (new LUDecomposition(m)).getDeterminant
  }

  def isCollinear(a: Coordinate, b: Coordinate, c: Coordinate): Boolean = {
    isCollinear(a.x, a.y, b.x, b.y, c.x, c.y)
  }

  def isCollinear(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double
  ): Boolean = {
    math.abs(ShewchuksDeterminant.orient2d(ax, ay, bx, by, cx, cy)) < EPSILON
  }

  def isCCW(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double
  ): Boolean = {
    // det [ a.x-c.x  a.y-c.y ]
    //     [ b.x-c.x  b.y-c.y ] > 0
    ShewchuksDeterminant.orient2d(ax, ay, bx, by, cx, cy) > EPSILON
  }

  def isCCW(a: Coordinate, b: Coordinate, c: Coordinate): Boolean =
    isCCW(a.x, a.y, b.x, b.y, c.x, c.y)

  def inCircle(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double,
    dx: Double, dy: Double
  ): Boolean = {
    // det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
    //      b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
    //      c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > EPSILON
    ShewchuksDeterminant.incircle(ax, ay, bx, by, cx, cy, dx, dy) > EPSILON
  }

  def inCircle(a: Coordinate, b: Coordinate, c: Coordinate, d: Coordinate): Boolean =
    inCircle(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y)


}

final class Predicates(pointSet: DelaunayPointSet, halfEdgeTable: HalfEdgeTable) {
  import pointSet._
  import halfEdgeTable._
  import Predicates._

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
    val det = ShewchuksDeterminant.orient2d(e0x, e0y, e1x, e1y, ptx, pty)

    if(det > EPSILON)
      LEFTOF
    else if(det < -EPSILON)
      RIGHTOF
    else
      ON
  }

  // final def isRightOf(e: Int, p: Coordinate) = {
  //   isCCW(p, trans(het.getDest(e)), trans(het.getSrc(e)))
  // }

  def isCCW(a: Int, b: Int, c: Int) = {
    val ax = getX(a)
    val ay = getY(a)
    val bx = getX(b)
    val by = getY(b)
    val cx = getX(c)
    val cy = getY(c)
    Predicates.isCCW(ax, ay, bx, by, cx, cy)
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
    Predicates.isCCW(px, py, bx, by, cx, cy)
  }

  // def isLeftOf(e: Int, p: Coordinate)(implicit trans: Int => Coordinate, het: HalfEdgeTable) =
  //   isCCW(p, trans(het.getSrc(e)), trans(het.getDest(e)))

  def isLeftOf(e: Int, p: Int) = {
    val px = getX(p)
    val py = getY(p)
    val b = getSrc(e)
    val bx = getX(b)
    val by = getY(b)
    val c = getDest(e)
    val cx = getX(c)
    val cy = getY(c)
    Predicates.isCCW(px, py, bx, by, cx, cy)
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
    Predicates.inCircle(ax, ay, bx, by, cx, cy, dx, dy)
  }

  // def circleCenter(a: Coordinate, b: Coordinate, c: Coordinate): Coordinate = {
  //   val d = 2.0 * det3(a.x, a.y, 1.0,
  //                      b.x, b.y, 1.0,
  //                      c.x, c.y, 1.0)
  //   val h = det3(a.x * a.x + a.y * a.y, a.y, 1.0,
  //                b.x * b.x + b.y * b.y, b.y, 1.0,
  //                c.x * c.x + c.y * c.y, c.y, 1.0) / d
  //   val k = det3(a.x, a.x * a.x + a.y * a.y, 1.0,
  //                b.x, b.x * b.x + b.y * b.y, 1.0,
  //                c.x, c.x * c.x + c.y * c.y, 1.0) / d
  //   new Coordinate(h,k)
  // }

  def circleCenter(ai: Int, bi: Int, ci: Int): Coordinate = {
    val ax = getX(ai)
    val ay = getY(ai)
    val bx = getX(bi)
    val by = getY(bi)
    val cx = getX(ci)
    val cy = getY(ci)

    val d = 2.0 * det3(ax, ay, 1.0,
                       bx, by, 1.0,
                       cx, cy, 1.0)
    val h = det3(ax * ax + ay * ay, ay, 1.0,
                 bx * bx + by * by, by, 1.0,
                 cx * cx + cy * cy, cy, 1.0) / d
    val k = det3(ax, ax * ax + ay * ay, 1.0,
                 bx, bx * bx + by * by, 1.0,
                 cx, cx * cx + cy * cy, 1.0) / d

    new Coordinate(h,k)
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
