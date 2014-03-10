package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */
object Unequal extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) = 
    if(z1 == z2) 0 else 1

  def combine(z1:Double,z2:Double):Double =
    if(isNoData(z1)) { if(isNoData(z2)) 0 else 1 }
    else {
      if(isNoData(z2)) { 1 }
      else { 
        if(z1 == z2) 0
        else 1
      }
    }
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
   * double, else 0.
   */
  def !==(d:Double) = localUnequal(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==:(d:Double) = localUnequal(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are not equal, else 0.
   */
  def localUnequal(rs:RasterSource) = self.combineOp(rs)(Unequal(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are not equal, else 0.
   */
  def !==(rs:RasterSource) = localUnequal(rs)
}

trait UnequalMethods { self: Raster =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localUnequal(i: Int) = Unequal(self, i)
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
  def localUnequal(d: Double) = Unequal(self, d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==(d:Double) = localUnequal(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==:(d:Double) = localUnequal(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are not equal, else 0.
   */
  def localUnequal(r:Raster) = Unequal(self,r)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the raster are not equal, else 0.
   */
  def !==(r:Raster) = localUnequal(r)
}
