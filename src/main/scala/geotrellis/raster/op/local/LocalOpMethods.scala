package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait LocalOpMethods[+Repr <: RasterDS] 
  extends LocalMapOpMethods[Repr]
     with AddOpMethods[Repr]
     with SubtractOpMethods[Repr]
     with MultiplyOpMethods[Repr]
     with DivideOpMethods[Repr] 
     with AndOpMethods[Repr] 
     with OrOpMethods[Repr] 
     with XorOpMethods[Repr] 
     with MinOpMethods[Repr] 
     with MaxOpMethods[Repr] 
     with PowOpMethods[Repr] 
     with EqualOpMethods[Repr] 
     with UnequalOpMethods[Repr] 
     with GreaterOpMethods[Repr]
     with LessOpMethods[Repr] 
     with GreaterOrEqualOpMethods[Repr] 
     with LessOrEqualOpMethods[Repr] 
     with ConditionalOpMethods[Repr] 
     with MajorityOpMethods[Repr] 
     with MinorityOpMethods[Repr] 
     with VarietyOpMethods[Repr] { self: Repr =>
  /** Get the negation of this raster source. Will convert double values into integers. */
  def localNot() = mapOp(Not(_))
  /** Get the negation of this raster source. Will convert double values into integers. */
  def unary_~() = localNot()

  /** Negate (multiply by -1) each value in a raster. */
  def localNegate() = mapOp(Negate(_))
  /** Negate (multiply by -1) each value in a raster. */
  def unary_-() = localNegate()

  /** Takes the Ceiling of each raster cell value. */
  def localCeil() = mapOp(Ceil(_))

  /** Takes the Floor of each raster cell value. */
  def localFloor() = mapOp(Floor(_))

  /** Computes the Log of a Raster. */
  def localLog() = mapOp(Log(_))

  /** Round the values of a Raster. */
  def localRound() = mapOp(Round(_))

  /** Take the square root each value in a raster. */
  def localSqrt() = mapOp(Sqrt(_))

  /** Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined() = mapOp(Defined(_))

  /** Maps an integer typed Raster to 0 if the cell value is not NODATA, otherwise 1. */
  def localUndefined() = mapOp(Undefined(_))

  /** Masks this raster based on cell values of the second raster. See [[Mask]]. */
  def localMask(rs:RasterDS,readMask:Int,writeMask:Int) = 
    combineOp(rs)(Mask(_,_,readMask,writeMask))

  /** InverseMasks this raster based on cell values of the second raster. See [[InverseMask]]. */
  def localInverseMask(rs:RasterDS,readMask:Int,writeMask:Int) = 
    combineOp(rs)(InverseMask(_,_,readMask,writeMask))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss:Seq[RasterDataSource]):RasterDataSource = 
    combineOp(rss)(Mean(_))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss:RasterDataSource*)(implicit d:DI):RasterDataSource = 
    localMean(rss)
}
