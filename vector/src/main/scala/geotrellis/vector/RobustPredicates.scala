package geotrellis.vector

import com.vividsolutions.jts.geom.Coordinate
import org.apache.commons.math3.linear._

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}


object RobustPredicates {

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
    // val d = 2.0 * det3(ax, ay, 1.0,
    //                    bx, by, 1.0,
    //                    cx, cy, 1.0)
    // val h = det3(ax * ax + ay * ay, ay, 1.0,
    //              bx * bx + by * by, by, 1.0,
    //              cx * cx + cy * cy, cy, 1.0) / d
    // val k = det3(ax, ax * ax + ay * ay, 1.0,
    //              bx, bx * bx + by * by, 1.0,
    //              cx, cx * cx + cy * cy, 1.0) / d

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
