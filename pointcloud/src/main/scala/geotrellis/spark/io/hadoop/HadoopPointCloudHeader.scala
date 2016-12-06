package geotrellis.spark.io.hadoop

import geotrellis.spark.io.PointCloudHeader
import org.apache.hadoop.fs.Path

case class HadoopPointCloudHeader(fileName: Path, metadata: String, schema: String) extends PointCloudHeader
