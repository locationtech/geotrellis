package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are less than other values. Sets to 1 if true, else 0.
 */
object Less extends LocalRasterBinaryOp {
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkLess(_,c))(checkLess(_,c)) }
         .withName("Less[ConstantInt]")

  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * intenger, else 0.
   */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(checkLess(_,c))(checkLess(_,c)) }
         .withName("Less[ConstantDouble]")

  private def checkLess(i1:Int,i2:Int) = 
    if(i1 < i2) 1 else 0

  private def checkLess(i:Int,d:Double) =
    if(isNaN(d)) { 0 }
    else {
      if(i == NODATA) { 0 }
      else { 
        if(i < d.toInt) 1
        else 0
      }
    }

  private def checkLess(d1:Double,d2:Double):Double = 
    if(isNaN(d1)) { 0 }
    else {
      if(isNaN(d2)) { 0 }
      else { 
        if(d1 < d2) 1 
        else 0 
      }
    }

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)(checkLess)(checkLess)
}

trait LessOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def localLess(i: Int) = self.mapOp(Less(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def <(i:Int) = localLess(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(i:Int) = localLess(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * intenger, else 0.
   */
  def localLess(d: Double) = self.mapOp(Less(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * intenger, else 0.
   */
  def <(d:Double) = localLess(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * intenger, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(d:Double) = localLess(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * intenger, else 0.
   */
  def localLess(rs:RasterDataSource) = self.combineOp(rs)(Less(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * intenger, else 0.
   */
  def <(rs:RasterDataSource) = localLess(rs)
}
