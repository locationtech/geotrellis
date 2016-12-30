package geotrellis.pointcloud.spark.dem

import io.pdal._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._
import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withPointCloudToDemMethods[M: GetComponent[?, LayoutDefinition]](
    self: RDD[(SpatialKey, PointCloud)] with Metadata[M]
  ) extends PointCloudToDemMethods[M](self)

  implicit class withPointCloudDemMethods(val self: PointCloud)
      extends PointCloudDemMethods
}
