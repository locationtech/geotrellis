package geotrellis.spark.pointcloud.triangulation

import io.pdal._
import geotrellis.vector.triangulation.DelaunayPointSet

object DelaunayPointCloudPointSet {

  implicit def pointCloudToDelaunayPointSet(pointCloud: PointCloud): DelaunayPointSet =
    new DelaunayPointSet {
      def length = pointCloud.length
      def getX(i: Int) = pointCloud.getX(i)
      def getY(i: Int) = pointCloud.getY(i)
      def getZ(i: Int) = pointCloud.getZ(i)
    }

}
