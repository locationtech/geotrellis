package trellis.operation

import trellis.raster.IntRaster


/**
  * Normalize the values in the given raster.
  * zmin and zmax are the lowest and highest values in the provided raster.
  * gmin and gmax are the desired minimum and maximum values in the output raster.
  */ 
case class Normalize2(r:IntRasterOperation, zmin:Int, zmax:Int,
                      gmin:Int, gmax:Int) extends NormalizeBase {
  def getMinMax(raster:IntRaster) = {
    if (this.zmin == this.zmax) {
      (this.zmax - 1, this.zmax)
    } else {
      (this.zmin, this.zmax)
    }
  }
}
