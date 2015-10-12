package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.etl._
import geotrellis.spark.ingest._

import scala.reflect.ClassTag

abstract class HadoopInput[I: IngestKey, K: ClassTag](implicit tiler: Tiler[I, K, Tile]) extends IngestInputPlugin[I, K] {
  val name = "hadoop"
  val requiredKeys = Array("path")
}