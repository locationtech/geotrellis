package geotrellis.spark.etl

import geotrellis.raster.MultiBandTile
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.spark.SpatialComponent
import geotrellis.spark.ingest._

import scala.reflect.runtime.universe._

object MultibandEtl extends IngestEtl[MultiBandTile] {
  def apply[
    I: ProjectedExtentComponent: TypeTag: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: TypeTag
  ](args: Seq[String], modules: Seq[TypedModule] = Etl.defaultModules) =
    Etl[I, K, MultiBandTile](args, modules)
}
