package geotrellis.spark.io

trait PointCloudHeader {
  val metadata: String
  val schema: String
}
