package geotrellis.spark.pointcloud.dem

import geotrellis.util.MethodExtensions

import io.pdal._


trait PointCloudDemMethods extends MethodExtensions[PointCloud] {

  /**
    * Compute the union of this PointCloud and the other one.
    */
  def union(other: Any): PointCloud = {
    val otherCloud = other match {
      case other: PointCloud => other
      case _ => throw new Exception
    }

    require(self.dimTypes == otherCloud.dimTypes)
    require(self.metadata == otherCloud.metadata)
    require(self.schema == otherCloud.schema)

    new PointCloud(self.bytes ++ otherCloud.bytes, self.dimTypes, self.metadata, self.schema)
  }

}
