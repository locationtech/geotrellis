package geotrellis.spark.pointcloud.triangulation

import io.pdal._
import geotrellis.spark.pointcloud.Point3D
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import org.apache.commons.math3.linear._

trait DelaunayPointSet {
  def length: Int
  def getX(i: Int): Double
  def getY(i: Int): Double
  def getZ(i: Int): Double
}

object DelaunayPointSet {

  implicit def pointCloudToDelaunayPointSet(pointCloud: PointCloud): DelaunayPointSet =
    new DelaunayPointSet {
      def length = pointCloud.length
      def getX(i: Int) = pointCloud.getX(i)
      def getY(i: Int) = pointCloud.getY(i)
      def getZ(i: Int) = pointCloud.getZ(i)
    }

  implicit def lightPointArrayToDelaunayPointSet(points: Array[Point3D]) =
    new DelaunayPointSet {
      def length = points.length
      def getX(i: Int) = points(i).x
      def getY(i: Int) = points(i).y
      def getZ(i: Int) = points(i).z
    }
}

class PointSetPredicates(pointSet: DelaunayPointSet, halfEdgeTable: HalfEdgeTable) {
  import geotrellis.vector.triangulation.ShewchuksDeterminant

  import pointSet.{getX, getY}
  import halfEdgeTable._

  def isCCW(v1: Int, v2: Int, v3: Int): Boolean =
    ShewchuksDeterminant.orient2d(
      getX(v1),
      getY(v1),
      getX(v2),
      getY(v2),
      getX(v3),
      getY(v3)
    ) > EPSILON

  def isLeftOf(e: Int, v: Int): Boolean =
    isCCW(v, getSrc(e), getVert(e))

  def isRightOf(e: Int, v: Int): Boolean =
    isCCW(v, getVert(e), getSrc(e))

  def isCollinear(e: Int, v: Int): Boolean = {
    val src = getSrc(e)
    val vert = getVert(e)

    isCollinear(src, vert, v)
  }

  def isCollinear(a: Int, b: Int, c: Int): Boolean =
    math.abs(
      ShewchuksDeterminant.orient2d(
        getX(a), getY(a),
        getX(b), getY(b),
        getX(c), getY(c)
      )
    ) < EPSILON

  def isCorner(edge: Int): Boolean = {
    !isCollinear(edge, getSrc(getPrev(edge))) || {
      val (cx, cy) = (getX(getSrc(edge)), getY(getSrc(edge)))
      val (nx, ny) = (getX(getVert(edge)), getY(getVert(edge)))
      val (px, py) = (getX(getSrc(getPrev(edge))), getY(getSrc(getPrev(edge))))
      val (xn, yn) = (nx - cx, ny - cy)
      val (xp, yp) = (px - cx, py - cy)
      xn * xp + yn * yp > 0
    }
  }

  def inCircle(a: Int, b: Int, c: Int, d: Int): Boolean =
    ShewchuksDeterminant.incircle(
      getX(a), getY(a),
      getX(b), getY(b),
      getX(c), getY(c),
      getX(d), getY(d)
    ) > EPSILON

  def circleCenter(a: Int, b: Int, c: Int): Point3D = {
    val ax = getX(a)
    val ay = getY(a)
    val bx = getX(b)
    val by = getY(b)
    val cx = getX(c)
    val cy = getY(c)

    val d = 2.0 * det3(ax, ay, 1.0,
                       bx, by, 1.0,
                       cx, cy, 1.0)
    val h = det3(ax * ax + ay * ay, ay, 1.0,
                 bx * bx + by * by, by, 1.0,
                 cx * cx + cy * cy, cy, 1.0) / d
    val k = det3(ax, ax * ax + ay * ay, 1.0,
                 bx, bx * bx + by * by, 1.0,
                 cx, cx * cx + cy * cy, 1.0) / d
    Point3D(h, k)
  }

  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    (new LUDecomposition(m)).getDeterminant
  }
}
