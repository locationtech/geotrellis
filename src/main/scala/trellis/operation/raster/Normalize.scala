package trellis.operation

import trellis.raster.IntRaster

/**
  * Normalize the values in the given raster so that all values are within the specified minimum and maximum value range.
  */
case class Normalize(r:IntRasterOperation,
                     gmin:Int, gmax:Int) extends NormalizeBase {
  def getMinMax(raster:IntRaster) = {
    val (zmin, zmax) = raster.findMinMax
    if (zmin == zmax) {
      (zmax - 1, zmax)
    } else {
      (zmin, zmax)
    }
  }
}
