package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl._

abstract class HadoopInput[I, V] extends InputPlugin[I, V] {
  val name = "hadoop"
}
