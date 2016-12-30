package geotrellis.pointcloud.spark.io.s3

import geotrellis.pointcloud.spark.io.PointCloudHeader

case class S3PointCloudHeader(key: String, metadata: String, schema: String) extends PointCloudHeader
