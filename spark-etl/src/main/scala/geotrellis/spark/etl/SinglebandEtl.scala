package geotrellis.spark.etl

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling.TilerKeyMethods

import scala.reflect.runtime.universe._

object SinglebandEtl {
  def apply[
    I: ProjectedExtentComponent: TypeTag: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: TypeTag](args: Seq[String], modules: Seq[TypedModule] = Etl.defaultModules) =
    Etl[I, K, Tile](args, modules)
}
