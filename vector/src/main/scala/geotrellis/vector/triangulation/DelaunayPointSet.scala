package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector.Point

trait DelaunayPointSet {
  def length: Int
  def getX(i: Int): Double
  def getY(i: Int): Double
  def getZ(i: Int): Double
  def getCoordinate(i: Int): Coordinate = new Coordinate(getX(i), getY(i), getZ(i))
  def getPoint(i: Int): Point = Point.jtsCoord2Point(getCoordinate(i))
  def apply(i: Int): Coordinate = getCoordinate(i)
  def distance(i1: Int, i2: Int): Double = {
    val dx = getX(i1) - getX(i2)
    val dy = getY(i1) - getY(i2)

    math.sqrt((dx * dx) + (dy * dy))
  }
}

object DelaunayPointSet {

  def apply(points: Array[Coordinate]): DelaunayPointSet =
    new DelaunayPointSet {
      def length = points.length
      def getX(i: Int) = points(i).x
      def getY(i: Int) = points(i).y
      def getZ(i: Int) = points(i).z
      override def getCoordinate(i: Int) = points(i)
    }

  def apply(points: Map[Int, Coordinate]): DelaunayPointSet =
    apply(points, points.size)

  def apply(points: Int => Coordinate, len: Int): DelaunayPointSet =
    new DelaunayPointSet {
      def length = len
      def getX(i: Int) = points(i).x
      def getY(i: Int) = points(i).y
      def getZ(i: Int) = points(i).z
      override def getCoordinate(i: Int) = points(i)
    }

  // implicit def pointCloudToDelaunayPointSet(pointCloud: PointCloud): DelaunayPointSet =
  //   new DelaunayPointSet {
  //     def length = pointCloud.length
  //     def getX(i: Int) = pointCloud.getX(i)
  //     def getY(i: Int) = pointCloud.getY(i)
  //     def getZ(i: Int) = pointCloud.getZ(i)
  //   }

  implicit def coordinateArrayToDelaunayPointSet(points: Array[Coordinate]): DelaunayPointSet =
    apply(points)
}
