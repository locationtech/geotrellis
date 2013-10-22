package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait LocalOpMethods[+Repr <: RasterDataSource] 
  extends AddOpMethods[Repr]
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
     with LessOpMethods[Repr] { self: Repr =>
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

  def localCombinations(rs:RasterDataSource) = combineOp(rs)(Combination(_,_))
}
