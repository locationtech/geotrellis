package geotrellis.pointcloud.spark.io.hadoop

import geotrellis.pointcloud.spark.io.PointCloudHeader
import org.apache.hadoop.fs.Path

case class HadoopPointCloudHeader(fileName: Path, metadata: String, schema: String) extends PointCloudHeader
