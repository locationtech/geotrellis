package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are less than or equal to other values. Sets to 1 if true, else 0.
 */
object LessOrEqual extends LocalRasterBinaryOp {
  /** Apply this operation to the values of each cell in each raster.  */
  override
  def combine(rasters:RasterSeq):Raster =
    rasters
      .dualCombine({ zs =>
        val z = zs(0)
        if(zs.tail.foldLeft(true)(_ && z <= _)) 1
        else 0
      })({ zs =>
        val z = zs(0)
        if(zs.tail.foldLeft(true)(_ && z <= _)) 1
        else 0
      })

  def combine(z1:Int,z2:Int) =
    if(z1 <= z2) 1 else 0

  def combine(z1:Double,z2:Double):Double =
    if(isNoData(z1)) { 0 }
    else {
      if(isNoData(z2)) { 0 }
      else { 
        if(z1 <= z2) 1 
        else 0 
      }
    }
}

trait LessOrEqualOpMethods[+Repr <: RasterSource] { self: Repr =>
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
   * double, else 0.
   */
  def localLessOrEqual(d: Double) = self.mapOp(LessOrEqual(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=(d:Double) = localLessOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=:(d:Double) = localLessOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def localLessOrEqual(rs:RasterSource) = self.combineOp(rs)(LessOrEqual(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def <=(rs:RasterSource) = localLessOrEqual(rs)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def localLessOrEqual(rss:Seq[RasterSource]) = self.combineOp(rss)(LessOrEqual(_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def <=(rss:Seq[RasterSource]) = localLessOrEqual(rss)
}
