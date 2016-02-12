package geotrellis.spark.etl

import geotrellis.raster.Tile
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.spark.SpatialComponent
import geotrellis.spark.ingest._

import scala.reflect.runtime.universe._

object SinglebandEtl extends IngestEtl[Tile] {
  def apply[
    I: ProjectedExtentComponent: TypeTag: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: TypeTag
  ](args: Seq[String], modules: Seq[TypedModule] = Etl.defaultModules) =
    Etl[I, K, Tile](args, modules)
}
