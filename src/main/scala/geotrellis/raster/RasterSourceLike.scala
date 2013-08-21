package geotrellis.raster

import geotrellis._
import geotrellis.raster.op._

trait RasterSourceLike[+Repr <: DataSource[Raster]] extends DataSourceLike[Raster, Repr] with DataSource[Raster] { self: Repr =>
  type CBF[A] = ({ type BF = CBSF[Repr, Raster, A]})#BF
  def tiles = self.partitions
  def rasterDefinition:Op[RasterDefinition]

  def get = {
    rasterDefinition.flatMap { rd =>
      val re = rd.re
      logic.Collect(rd.tiles).map(s => Raster(TileArrayRasterData(s.toArray, rd.tileLayout, re),re))
    }}
      
      
def converge = LocalRasterSource.fromRaster(
    rasterDefinition.flatMap { rd =>
      val re = rd.re
      logic.Collect(rd.tiles).map(s => Raster(TileArrayRasterData(s.toArray,rd.tileLayout,re), re))
    })
 
  def localAdd[That](i: Int)(implicit cbf: CBF[That]) = this.map(local.Add(_, i))
  def localSubtract[That](i: Int)(implicit cbf: CBF[That]) = this.map(local.Subtract(_, i))

}



