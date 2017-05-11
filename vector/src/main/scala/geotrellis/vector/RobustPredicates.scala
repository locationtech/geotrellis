package geotrellis.vector

import com.vividsolutions.jts.geom.Coordinate
import org.apache.commons.math3.linear._

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}


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

  def isCollinear(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double
  ): Boolean = {
    math.abs(ShewchuksDeterminant.orient2d(ax, ay, bx, by, cx, cy)) < EPSILON
  }

  def isCollinear(a: Coordinate, b: Coordinate, c: Coordinate): Boolean = {
    isCollinear(a.x, a.y, b.x, b.y, c.x, c.y)
  }

  def isCCW(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double
  ): Boolean = {
    ShewchuksDeterminant.orient2d(ax, ay, bx, by, cx, cy) > EPSILON
  }

  def isCCW(a: Coordinate, b: Coordinate, c: Coordinate): Boolean =
    isCCW(a.x, a.y, b.x, b.y, c.x, c.y)

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

  def inCircle(a: Coordinate, b: Coordinate, c: Coordinate, d: Coordinate): Boolean =
    inCircle(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y)

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
