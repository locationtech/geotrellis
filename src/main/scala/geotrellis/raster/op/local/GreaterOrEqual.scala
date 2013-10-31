package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are greater than or equal to other values. Sets to 1 if true, else 0.
 */
object GreaterOrEqual extends LocalRasterBinaryOp {
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * integer, else 0.
   */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkGreaterOrEqual(_,c))(checkGreaterOrEqual(_,c)) }
         .withName("GreaterOrEqual[ConstantInt]")

  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * intenger, else 0.
   */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkGreaterOrEqual(_,c))(checkGreaterOrEqual(_,c)) }
         .withName("GreaterOrEqual[ConstantDouble]")

  private def checkGreaterOrEqual(i1:Int,i2:Int) = 
    if(i1 >= i2) 1 else 0

  private def checkGreaterOrEqual(i:Int,d:Double) =
    if(isNaN(d)) { 0 }
    else {
      if(i == NODATA) { 0 }
      else { 
        if(i >= d.toInt) 1
        else 0
      }
    }

  private def checkGreaterOrEqual(d1:Double,d2:Double):Double = 
    if(isNaN(d1)) { 0 }
    else {
      if(isNaN(d2)) { 0 }
      else { 
        if(d1 >= d2) 1 
        else 0 
      }
    }

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)(checkGreaterOrEqual)(checkGreaterOrEqual)
}

trait GreaterOrEqualOpMethods[+Repr <: RasterSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * integer, else 0.
   */
  def localGreaterOrEqual(i: Int) = self.mapOp(GreaterOrEqual(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
    * integer, else 0.
   */
  def >=(i:Int) = localGreaterOrEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * integer, else 0.
   */
  def >=:(i:Int) = localGreaterOrEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * intenger, else 0.
   */
  def localGreaterOrEqual(d: Double) = self.mapOp(GreaterOrEqual(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * intenger, else 0.
   */
  def >=(d:Double) = localGreaterOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * intenger, else 0.
   */
  def >=:(d:Double) = localGreaterOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * intenger, else 0.
   */
  def localGreaterOrEqual(rs:RasterSource) = self.combineOp(rs)(GreaterOrEqual(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * intenger, else 0.
   */
  def >=(rs:RasterSource) = localGreaterOrEqual(rs)
}
