package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are less than or equal to other values. Sets to 1 if true, else 0.
 */
object LessOrEqual extends LocalRasterBinaryOp {
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkLessOrEqual(_,c))(checkLessOrEqual(_,c)) }
         .withName("LessOrEqual[ConstantInt]")

  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * intenger, else 0.
   */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkLessOrEqual(_,c))(checkLessOrEqual(_,c)) }
         .withName("LessOrEqual[ConstantDouble]")

  private def checkLessOrEqual(i1:Int,i2:Int) = 
    if(i1 <= i2) 1 else 0

  private def checkLessOrEqual(i:Int,d:Double) =
    if(isNaN(d)) { 0 }
    else {
      if(i == NODATA) { 0 }
      else { 
        if(i <= d.toInt) 1
        else 0
      }
    }

  private def checkLessOrEqual(d1:Double,d2:Double):Double = 
    if(isNaN(d1)) { 0 }
    else {
      if(isNaN(d2)) { 0 }
      else { 
        if(d1 <= d2) 1 
        else 0 
      }
    }

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)(checkLessOrEqual)(checkLessOrEqual)
}

trait LessOrEqualOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqual(i: Int) = self.mapOp(LessOrEqual(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
    * integer, else 0.
   */
  def <=(i:Int) = localLessOrEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def <=:(i:Int) = localLessOrEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * intenger, else 0.
   */
  def localLessOrEqual(d: Double) = self.mapOp(LessOrEqual(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * intenger, else 0.
   */
  def <=(d:Double) = localLessOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * intenger, else 0.
   */
  def <=:(d:Double) = localLessOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * intenger, else 0.
   */
  def localLessOrEqual(rs:RasterDataSource) = self.combineOp(rs)(LessOrEqual(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * intenger, else 0.
   */
  def <=(rs:RasterDataSource) = localLessOrEqual(rs)
}
