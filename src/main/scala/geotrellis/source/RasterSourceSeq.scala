package geotrellis.source

import geotrellis._
import geotrellis.raster.op.local._

/** This class gives the ability to apply a local operation
  * that reduces a sequence of rasters to a set of rasters
  * that are loaded in parallel. So if you wanted to group
  * the raster loading by loading some number of RasterSources
  * simultaneously, use this class to group the RasterSources.
  */
case class RasterSourceSeq(seq:Seq[RasterSource]) {
  val rasterDefinition = seq.head.rasterDefinition

  def applyOp(f:Seq[Op[Raster]]=>Op[Raster]) = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(rasterDefinition)
    builder.setOp {
      seq.map(_.tiles).mapOps(_.transpose.map(f))
    }
    builder.result
  }

  /** Adds all the rasters in the sequence */
  def localAdd():RasterSource = applyOp(Add(_))

  /** Takes the difference of the rasters in the sequence from left to right */
  def difference() = localSubtract
  /** Takes the difference of the rasters in the sequence from left to right */
  def localSubtract() = applyOp(Subtract(_))
  /** Takes the product of the rasters in the sequence */
  def product() = localMultiply
  /** Takes the product of the rasters in the sequence */
  def localMultiply() = applyOp(Multiply(_))
  /** Divides the rasters in the sequence from left to right */
  def localDivide() = applyOp(Multiply(_))

  /** Takes the max of each cell value */
  def max() = applyOp(Max(_))
  /** Takes the min of each cell value */
  def min() = applyOp(Min(_))

  /** Takes the logical And of each cell value */
  def and() = applyOp(And(_))
  /** Takes the logical Or of each cell value */
  def or() = applyOp(Or(_))
  /** Takes the logical Xor of each cell value */
  def xor() = applyOp(Xor(_))

  /** Raises each cell value to the power of the next raster, from left to right */
  def exponentiate() = applyOp(Pow(_))
}
