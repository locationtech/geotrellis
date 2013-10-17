package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are greater than other values. Sets to 1 if true, else 0.
 */
object Greater extends LocalRasterBinaryOp {
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkGreater(_,c))(checkGreater(_,c)) }
         .withName("Greater[ConstantInt]")

  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * intenger, else 0.
   */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkGreater(_,c))(checkGreater(_,c)) }
         .withName("Greater[ConstantDouble]")

  private def checkGreater(i1:Int,i2:Int) = 
    if(i1 > i2) 1 else 0

  private def checkGreater(i:Int,d:Double) =
    if(isNaN(d)) { 0 }
    else {
      if(i == NODATA) { 0 }
      else { 
        if(i > d.toInt) 1
        else 0
      }
    }

  private def checkGreater(d1:Double,d2:Double):Double = 
    if(isNaN(d1)) { 0 }
    else {
      if(isNaN(d2)) { 0 }
      else { 
        if(d1 > d2) 1 
        else 0 
      }
    }

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)(checkGreater)(checkGreater)
}

trait GreaterOpMethods[+Repr <: RasterSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreater(i: Int) = self.mapOp(Greater(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def >(i:Int) = localGreater(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(i:Int) = localGreater(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * intenger, else 0.
   */
  def localGreater(d: Double) = self.mapOp(Greater(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * intenger, else 0.
   */
  def >(d:Double) = localGreater(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * intenger, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d:Double) = localGreater(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * intenger, else 0.
   */
  def localGreater(rs:RasterSource) = self.combineOp(rs)(Greater(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * intenger, else 0.
   */
  def >(rs:RasterSource) = localGreater(rs)
}
