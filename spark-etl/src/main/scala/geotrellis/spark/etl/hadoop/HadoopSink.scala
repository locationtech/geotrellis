package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl.SinkPlugin

trait HadoopSink extends SinkPlugin {
  val name = "hadoop"
  val requiredKeys = Array("path")
}
