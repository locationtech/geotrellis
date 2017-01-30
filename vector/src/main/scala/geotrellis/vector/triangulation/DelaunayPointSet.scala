package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}


trait DelaunayPointSet {
  def length: Int
  def getX(i: Int): Double
  def getY(i: Int): Double
  def getZ(i: Int): Double
  def getCoordinate(i: Int): Coordinate = new Coordinate(getX(i), getY(i), getZ(i))
}

object DelaunayPointSet {

  // implicit def pointCloudToDelaunayPointSet(pointCloud: PointCloud): DelaunayPointSet =
  //   new DelaunayPointSet {
  //     def length = pointCloud.length
  //     def getX(i: Int) = pointCloud.getX(i)
  //     def getY(i: Int) = pointCloud.getY(i)
  //     def getZ(i: Int) = pointCloud.getZ(i)
  //   }

  implicit def coordinateArrayToDelaunayPointSet(points: Array[Coordinate]): DelaunayPointSet =
    new DelaunayPointSet {
      def length = points.length
      def getX(i: Int) = points(i).x
      def getY(i: Int) = points(i).y
      def getZ(i: Int) = points(i).z
      override def getCoordinate(i: Int) = points(i)
    }
}
