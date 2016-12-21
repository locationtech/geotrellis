package geotrellis.spark.pointcloud.triangulation

import io.pdal._
import geotrellis.spark.pointcloud.Point3D
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}

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
}
