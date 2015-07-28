package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl.OutputPlugin

trait HadoopOutput extends OutputPlugin {
  val name = "hadoop"
  val requiredKeys = Array("path")
}
