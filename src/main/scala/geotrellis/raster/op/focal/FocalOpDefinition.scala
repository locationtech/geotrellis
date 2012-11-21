package geotrellis.raster.op.focal

import geotrellis._

/**
 * Defines methods to create new FocalCalculations and FocalOpDatas
 * that define how a focal operation
 */
sealed trait FocalOpDefinition

trait IntFocalOpDefinition extends FocalOpDefinition {
  def newCalc: FocalCalculation[Int]
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Int or
   *  extend not matching the input raster */
  def newData(r: Raster): FocalOpData[Int] = IntFocalOpData(r.rasterExtent)
}

trait DoubleFocalOpDefinition extends FocalOpDefinition {
  def newCalc: FocalCalculation[Double]
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Double
   *  extend not matching the input raster */
  def newData(r: Raster): FocalOpData[Double] = DoubleFocalOpData(r.rasterExtent)
}

trait MultiTypeFocalOpDefinition extends FocalOpDefinition {
  def newIntCalc: FocalCalculation[Int]
  def newDoubleCalc: FocalCalculation[Double]
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Int or
   *  extend not matching the input raster */
  def newIntData(r: Raster): FocalOpData[Int] = IntFocalOpData(r.rasterExtent)
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Double or
   *  extend not matching the input raster */
  def newDoubleData(r: Raster): FocalOpData[Double] = DoubleFocalOpData(r.rasterExtent)
}


