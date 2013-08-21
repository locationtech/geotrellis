package geotrellis.raster

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._

trait RasterSourceLike[+Repr <: DataSource[Raster]] extends DataSourceLike[Raster, Repr] with DataSource[Raster] { self: Repr =>
  type CBF[A] = ({ type BF = CBSF[Repr, Raster, A]})#BF
  type HBF[A] = ({ type BF = CBSF[Repr, statistics.Histogram, A] })#BF
  //type CBF[A] = ({ type BF = CBSF[Repr, Raster, A]})#BF
  def tiles = self.partitions
  def rasterDefinition:Op[RasterDefinition]

  def get = {
    rasterDefinition.flatMap { rd =>
      val re = rd.re
      logic.Collect(rd.tiles).map(s => Raster(TileArrayRasterData(s.toArray, rd.tileLayout, re),re))
    }}    
  
  def converge = LocalRasterSource.fromRaster(this.get)
  
  // examples of transformations that retain this source type
  def localAdd[That](i: Int)(implicit cbf: CBF[That]) = this.map(local.Add(_, i))
  def localSubtract[That](i: Int)(implicit cbf: CBF[That]) = this.map(local.Subtract(_, i))

  // examples that become a local source type eventually but can act on any type
  def histogram[That](implicit cbf:HBF[That]) = this.map(stat.GetHistogram(_))
}



