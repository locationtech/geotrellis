package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl._

trait HadoopInput extends InputPlugin {
  val name = "hadoop"
  val requiredKeys = Array("path")
}