package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are greater than other values. Sets to 1 if true, else 0.
 */
object Greater extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if(z1 > z2) 1 else 0

  def combine(z1:Double,z2:Double):Double =
    if(isNoData(z1)) { 0 }
    else {
      if(isNoData(z2)) { 0 }
      else { 
        if(z1 > z2) 1 
        else 0 
      }
    }
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
  def localGreaterRightAssociative(i: Int) = self.mapOp(Greater(i, _))
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
  def >>:(i:Int) = localGreaterRightAssociative(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreater(d: Double) = self.mapOp(Greater(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreaterRightAssociative(d: Double) = self.mapOp(Greater(d, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def >(d:Double) = localGreater(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d:Double) = localGreaterRightAssociative(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def localGreater(rs:RasterSource) = self.combineOp(rs)(Greater(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def >(rs:RasterSource) = localGreater(rs)
}

trait GreaterMethods { self: Raster =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreater(i: Int) = Greater(self, i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreaterRightAssociative(i: Int) = Greater(i, self)
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
  def >>:(i:Int) = localGreaterRightAssociative(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreater(d: Double) = Greater(self, d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreaterRightAssociative(d: Double) = Greater(d, self)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def >(d:Double) = localGreater(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d:Double) = localGreaterRightAssociative(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def localGreater(r:Raster) = Greater(self,r)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the raster are greater than the next raster, else 0.
   */
  def >(r:Raster) = localGreater(r)
}
