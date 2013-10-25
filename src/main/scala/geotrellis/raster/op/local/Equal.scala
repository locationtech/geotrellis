package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */
object Equal extends LocalRasterBinaryOp {
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkEqual(_,c))(checkEqual(_,c)) }
         .withName("Equal[ConstantInt]")

  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkEqual(_,c))(checkEqual(_,c)) }
         .withName("Equal[ConstantDouble]")

  private def checkEqual(i1:Int,i2:Int) = 
    if(i1 == i2) 1 else 0

  private def checkEqual(i:Int,d:Double) =
    if(isNaN(d)) { if(i == NODATA) 1 else 0 }
    else {
      if(i == NODATA) { 0 }
      else { 
        if(i == d.toInt) 1
        else 0
      }
    }

  private def checkEqual(d1:Double,d2:Double):Double = 
    if(isNaN(d1)) { if(isNaN(d2)) 1 else 0 }
    else {
      if(isNaN(d2)) { 0 }
      else { 
        if(d1 == d2) 1 
        else 0 
      }
    }

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)(checkEqual)(checkEqual)
}

trait EqualOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localEqual(i: Int) = self.mapOp(Equal(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def ===(i:Int) = localEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def ===:(i:Int) = localEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localEqual(d: Double) = self.mapOp(Equal(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def ===(d:Double) = localEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def ===:(d:Double) = localEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localEqual(rs:RasterDataSource) = self.combineOp(rs)(Equal(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def ===(rs:RasterDataSource) = localEqual(rs)
}
