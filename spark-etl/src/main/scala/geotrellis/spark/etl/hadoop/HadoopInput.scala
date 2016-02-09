package geotrellis.spark.etl.hadoop

import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.CellGrid
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject.TileReprojectMethods
import geotrellis.spark._
import geotrellis.spark.etl._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling._

import scala.reflect.ClassTag

abstract class HadoopInput[
  I: ProjectedExtentComponent: ? => TilerKeyMethods[I, K],
  K: SpatialComponent: ClassTag,
  V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TileReprojectMethods[V]: ? => TilePrototypeMethods[V]
] extends IngestInputPlugin[I, K, V] {
  val name = "hadoop"
  val requiredKeys = Array("path")
}
