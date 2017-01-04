package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate
import org.apache.commons.math3.linear._

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}

object Predicates {
  sealed trait Relation
  object LEFTOF extends Relation
  object RIGHTOF extends Relation
  object ON extends Relation

  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    (new LUDecomposition(m)).getDeterminant
  }

  def isCollinear(a: Coordinate, b: Coordinate, c: Coordinate): Boolean =
    math.abs(ShewchuksDeterminant.orient2d(a.x, a.y, b.x, b.y, c.x, c.y)) < EPSILON

  def isCollinear(e: Int, v: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Boolean = {
    val a = trans(het.getSrc(e))
    val b = trans(het.getDest(e))
    val c = trans(v)

    isCollinear(a, b, c)
  }

  def doesExtend(e: Int, v: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Boolean = {
    val a = trans(het.getSrc(e))
    val b = trans(het.getDest(e))
    val c = trans(v)

    val d1 = math.pow(a.x - b.x, 2) + math.pow(a.y - b.y, 2)
    val d2 = math.pow(a.x - c.x, 2) + math.pow(a.y - c.y, 2)

    d1 < d2
  }

  def isCorner(edge: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Boolean = {
    !isCollinear(edge, het.getSrc(het.getPrev(edge))) || {
      val c = trans(het.getSrc(edge))
      val n = trans(het.getDest(edge))
      val p = trans(het.getSrc(het.getPrev(edge)))
      val (xn, yn) = (n.x - c.x, n.y - c.y)
      val (xp, yp) = (p.x - c.x, p.y - c.y)
      xn * xp + yn * yp > 0
    }
  }

  def relativeTo(e: Int, p: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Relation = {
    import het._
    val e0 = trans(getSrc(e))
    val e1 = trans(getDest(e))
    val pt = trans(p)
    val det = ShewchuksDeterminant.orient2d(e0.x, e0.y, e1.x, e1.y, pt.x, pt.y)

    if(det > EPSILON)
      LEFTOF
    else if(det < -EPSILON)
      RIGHTOF
    else
      ON
  }

  def isCCW(a: Coordinate, b: Coordinate, c: Coordinate): Boolean = {
    // det [ a.x-c.x  a.y-c.y ]
    //     [ b.x-c.x  b.y-c.y ] > 0
    ShewchuksDeterminant.orient2d(a.x, a.y, b.x, b.y, c.x, c.y) > EPSILON
  }

  def isRightOf(e: Int, p: Coordinate)(implicit trans: Int => Coordinate, het: HalfEdgeTable) =
    isCCW(p, trans(het.getDest(e)), trans(het.getSrc(e)))

  def isRightOf(e: Int, p: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable) =
    isCCW(trans(p), trans(het.getDest(e)), trans(het.getSrc(e)))

  def isLeftOf(e: Int, p: Coordinate)(implicit trans: Int => Coordinate, het: HalfEdgeTable) =
    isCCW(p, trans(het.getSrc(e)), trans(het.getDest(e)))

  def isLeftOf(e: Int, p: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable) =
    isCCW(trans(p), trans(het.getSrc(e)), trans(het.getDest(e)))

  def inCircle(a: Coordinate, b: Coordinate, c: Coordinate, d: Coordinate): Boolean = {
    // det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
    //      b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
    //      c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > EPSILON
    ShewchuksDeterminant.incircle(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y) > EPSILON
  }

  def inCircle(ai: Int, bi: Int, ci: Int, di: Int)(implicit trans: Int => Coordinate): Boolean = {
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

  def circleCenter(ai: Int, bi: Int, ci: Int)(implicit trans: Int => Coordinate): Coordinate = {
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

  def isDelaunayEdge(e: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Boolean = {
    // Predicated on the fact that if an edge is Delaunay, then for a
    // point, A, to the left of edge (X,Y), and a point, B, to the
    // right of (X,Y), A may not be in the circle defined by points X,
    // Y, and B.
    import het._
    val a = trans(getDest(getNext(e)))
    val b = trans(getDest(getNext(getFlip(e))))
    val x = trans(getDest(getFlip(e)))
    val y = trans(getDest(e))
    !inCircle(a, x, y, b)
  }

  def isConvexBoundary(e0: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Boolean = {
    import het._
    var e = e0
    var valid = true
    do {
      valid = valid && !isLeftOf(e, getDest(getNext(e)))
      e = getNext(e)
    } while (valid && e != e0)
    valid
  }
}

