package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling._

import scala.reflect.ClassTag

abstract class HadoopInput[I: IngestKey: ? => TilerKeyMethods[I, K], K: SpatialComponent: ClassTag] extends IngestInputPlugin[I, K] {
  val name = "hadoop"
  val requiredKeys = Array("path")
}
