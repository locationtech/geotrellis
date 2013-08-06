package geotrellis.raster

import geotrellis._
import geotrellis.raster.op._

trait RasterSourceLike[+Repr <: DataSource[Raster]] extends DataSourceLike[Raster, Repr] with DataSource[Raster] { self: Repr =>
  def tiles = self.partitions

  def localAdd[That](i: Int)(implicit cbf: CanBuildSourceFrom[Repr, Raster, That]) = this.map(local.Add(_, i))
}