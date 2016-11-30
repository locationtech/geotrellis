package geotrellis.spark.pointcloud.dem

import io.pdal._


object Implicits extends Implicits

trait Implicits {

  implicit class withPointCloudDemMethods(val self: PointCloud)
      extends PointCloudDemMethods

}
