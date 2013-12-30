package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._

trait TileSummary[T,V,That <: DataSource[_,V]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):T
  def handleFullTile(pt:FullTileIntersection):T
  def converge(ds: DataSource[T,_]):That
}
