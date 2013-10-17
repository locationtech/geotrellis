package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */
object Unequal extends LocalRasterBinaryOp {
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkUnequal(_,c))(checkUnequal(_,c)) }
         .withName("Unequal[ConstantInt]")

  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkUnequal(_,c))(checkUnequal(_,c)) }
         .withName("Unequal[ConstantDouble]")

  private def checkUnequal(i1:Int,i2:Int) = 
    if(i1 == i2) 0 else 1

  private def checkUnequal(i:Int,d:Double) =
    if(isNaN(d)) { if(i == NODATA) 0 else 1 }
    else {
      if(i == NODATA) { 1 }
      else { 
        if(i == d.toInt) 0
        else 1
      }
    }

  private def checkUnequal(d1:Double,d2:Double):Double = 
    if(isNaN(d1)) { if(isNaN(d2)) 0 else 1 }
    else {
      if(isNaN(d2)) { 1 }
      else { 
        if(d1 == d2) 0
        else 1
      }
    }

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)(checkUnequal)(checkUnequal)
}

trait UnequalOpMethods[+Repr <: RasterSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localUnequal(i: Int) = self.mapOp(Unequal(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==(i:Int) = localUnequal(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==:(i:Int) = localUnequal(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localUnequal(d: Double) = self.mapOp(Unequal(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def !==(d:Double) = localUnequal(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def !==:(d:Double) = localUnequal(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localUnequal(rs:RasterSource) = self.combineOp(rs)(Unequal(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def !==(rs:RasterSource) = localUnequal(rs)
}
