package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.data._
import geotrellis.raster._
import geotrellis.statistics._

//import RasterSource._

trait RasterSourceLike[+Repr <: RasterSource] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def get()(implicit mf:Manifest[Raster]) = {
    rasterDefinition flatMap { rd =>
      val re = rd.re
      logic.Collect(rd.tiles).map(s => Raster(TileArrayRasterData(s.toArray, rd.tileLayout, re),re))
    }}
  
  // Methods w/ transformations that retain this source type.
  def localAdd(i: Int) = this mapOp(local.Add(_, i))
  def localSubtract(i: Int):RasterSource = this mapOp(local.Subtract(_, i))

  // Methods that return a local source but can act on any type.
  def histogram():HistogramDS = this mapOp(stat.GetHistogram(_))

  // Methods that act on a local source.

}
