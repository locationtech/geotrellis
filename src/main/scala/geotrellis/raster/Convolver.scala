package geotrellis.raster

import geotrellis._

/**
 * Supplies functionaltiy to operations that do convolution.
 */
case class Convolver(rasterExtent:RasterExtent,k:Kernel) {

  val cols = rasterExtent.cols
  val rows = rasterExtent.rows

  val kraster = k.raster
  var kernelcols = kraster.cols
  var kernelrows = kraster.rows

  val data:IntArrayRasterData = IntArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)

  def stampKernel(col:Int,row:Int,z:Int) = {
    val rowmin = row - kernelrows / 2
    val rowmax = math.min(row + kernelrows / 2 + 1, rows)
 
    val colmin = col - kernelcols / 2
    val colmax = math.min(col + kernelcols / 2 + 1, cols)

    var kcol = 0
    var krow = 0

    var r = rowmin
    var c = colmin
    while(r < rowmax) {
      while(c < colmax) {               
        if (r >= 0 && c >= 0 && r < rows && c < cols &&
            kcol >= 0 && krow >= 0 && kcol < kernelcols && krow < kernelrows) {

          val k = kraster.get(kcol,krow)
          if (k != NODATA) {
            val o = data.get(c,r)
            val w = if (o == NODATA) {
              k * z
            } else {
              o + k*z
            }
            data.set(c,r,w)
          }
        } 

        c += 1
        kcol += 1
      }

      kcol = 0
      c = colmin
      r += 1
      krow += 1
    }
  }

  def result = Raster(data,rasterExtent)
}
