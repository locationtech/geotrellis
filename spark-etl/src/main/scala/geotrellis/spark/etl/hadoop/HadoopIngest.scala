package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl._

trait HadoopIngest extends IngestPlugin {
  val name = "hadoop"
  val requiredKeys = Array("path")
}