package geotrellis.spark.io

import geotrellis.spark.pointcloud.ProjectedExtent3D
import geotrellis.spark.pointcloud.json._

import spray.json._

trait PointCloudHeader {
  val metadata: String
  val schema: String

  def projectedExtent3D = metadata.parseJson.convertTo[ProjectedExtent3D]
  def extent3D = projectedExtent3D.extent3d
  def extent = projectedExtent3D.extent3d.toExtent
  def crs = projectedExtent3D.crs
}
