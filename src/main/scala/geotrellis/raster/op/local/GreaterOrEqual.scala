package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are greater than or equal to other values. Sets to 1 if true, else 0.
 */
object GreaterOrEqual extends LocalRasterBinaryOp {
  /** Apply this operation to the values of each cell in each raster.  */
  override
  def apply(rss:Seq[Op[Raster]]):Op[Raster] = 
    rss.mapOps { rasters =>
        rasters.head
               .dualCombine(rasters.tail)({ zs =>
                   val z = zs(0)
                   if(zs.tail.foldLeft(true)(_ && z >= _)) 1
                   else 0
                })({ zs =>
                  val z = zs(0)
                  if(zs.tail.foldLeft(true)(_ && z >= _)) 1
                  else 0
                })
       }
      .withName(s"GreaterOrEqual[Rasters]")

  def combine(z1:Int,z2:Int) =
    if(z1 >= z2) 1 else 0

  def combine(z1:Double,z2:Double):Double =
    if(isNaN(z1)) { 0 }
    else {
      if(isNaN(z2)) { 0 }
      else { 
        if(z1 >= z2) 1 
        else 0 
      }
    }
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
   * double, else 0.
   */
  def localGreaterOrEqual(d: Double) = self.mapOp(GreaterOrEqual(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * double, else 0.
   */
  def >=(d:Double) = localGreaterOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than or equal to the input
   * double, else 0.
   */
  def >=:(d:Double) = localGreaterOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than or equal to the next raster, else 0.
   */
  def localGreaterOrEqual(rs:RasterSource) = self.combineOp(rs)(GreaterOrEqual(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than or equal to the next raster, else 0.
   */
  def >=(rs:RasterSource) = localGreaterOrEqual(rs)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than or equal to the next raster, else 0.
   */
  def localGreaterOrEqual(rss:Seq[RasterSource]) = self.combineOp(rss)(GreaterOrEqual(_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than or equal to the next raster, else 0.
   */
  def >=(rss:Seq[RasterSource]) = localGreaterOrEqual(rss)
}
