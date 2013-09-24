package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.data._
import geotrellis.raster._

trait RasterSourceLike[+Repr <: DataSource[Raster,Raster]] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] { self: Repr =>

  type CBF[A] = ({ type BF = CBSF[Repr, Raster, A]})#BF
  type HBF[A] = ({ type BF = CBSF[Repr, statistics.Histogram, A] })#BF
  //type CBF[A] = ({ type BF = CBSF[Repr, Raster, A]})#BF
  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def get()(implicit mf:Manifest[Raster]) = {
    rasterDefinition flatMap { rd =>
      val re = rd.re
      logic.Collect(rd.tiles).map(s => Raster(TileArrayRasterData(s.toArray, rd.tileLayout, re),re))
    }}    
  
  // Methods w/ transformations that retain this source type.
  def localAdd[That](i: Int)(implicit cbf: CBF[That]) = this mapOp(local.Add(_, i))
  def localSubtract[That](i: Int)(implicit cbf: CBF[That]) = this mapOp(local.Subtract(_, i))

  // Methods that return a local source but can act on any type.

  /** histograms return a SeqSource, which represents a source that generates a sequence 
    * of values.  The SeqSource will be either a DistributedSeqSource or a LocalSeqSource
    * depending on the original source.  These histograms can processed in distributed
    * or parallel execution.
    */
  def histogram[That](implicit cbf:HBF[That]) = 
    this mapOp(stat.GetHistogram(_))

  // Methods that act on a local source.

  //?TODO: should we force here, or in png, or not at all?
  /*def renderPng(colorRamp: Op[ColorRamp]):ValueDataSource[Array[Byte]] = {
    val rasterOp = this.converge.get 
    val pngOp = io.SimpleRenderPng(rasterOp, colorRamp)
    new ValueDataSource(pngOp)
  }*/
}
