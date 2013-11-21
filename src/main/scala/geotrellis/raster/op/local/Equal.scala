package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */
object Equal extends LocalRasterBinaryOp {
  /** Apply this operation to the values of each cell in each raster.  */
  override
  def apply(rss:Seq[Op[Raster]]):Op[Raster] = 
    rss.mapOps { rasters =>
        rasters.head
               .dualCombine(rasters.tail)({ zs => 
                  if(zs.distinct.length < 2) 1 else 0
                })({ zs =>
                  if(zs.distinct.filter(isData(_)).length < 2) 1 else 0
                })
       }
      .withName(s"Equal[Rasters]")

  def combine(z1:Int,z2:Int) = 
    if(z1 == z2) 1 else 0

  def combine(z1:Double,z2:Double):Double =
    if(isNoData(z1)) { if(isNoData(z2)) 1 else 0 }
    else {
      if(isNoData(z2)) { 0 }
      else { 
        if(z1 == z2) 1 
        else 0 
      }
    }
}

trait EqualOpMethods[+Repr <: RasterSource] { self: Repr =>
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
   * double, else 0.
   */
  def localEqual(d: Double) = self.mapOp(Equal(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def ===(d:Double) = localEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def ===:(d:Double) = localEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are equal, else 0.
   */
  def localEqual(rs:RasterSource) = self.combineOp(rs)(Equal(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are equal, else 0.
   */
  def ===(rs:RasterSource) = localEqual(rs)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are equal, else 0.
   */
  def localEqual(rss:Seq[RasterSource]) = self.combineOp(rss)(Equal(_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are equal, else 0.
   */
  def ===(rss:Seq[RasterSource]) = localEqual(rss)
}
