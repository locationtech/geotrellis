package geotrellis.raster

import geotrellis._

/** 
 * Kernel
 *
 * Represents a neighborhood that is represented by
 * a raster.
 */
case class Kernel(raster:Raster) {
  if(raster.rows != raster.cols) sys.error("Kernel raster must be square")
  if(raster.rows % 2 != 1) sys.error("Kernel raster must have odd dimension")
}

object Kernel {
  implicit def raster2Kernel(r:Raster):Kernel = Kernel(r)
  implicit def raster2KernelOp(r:Raster):Op[Kernel] = Literal(Kernel(r))
  implicit def rasterOp2Kernel(r:Op[Raster]):Op[Kernel] = r.map(Kernel(_))

  /**
   * Creates a Gaussian kernel. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
   *
   * @param    size           Number of rows of the resulting raster.
   * @param    cellWidth      Cell width of the resulting raster
   * @param    sigma          Sigma parameter for Gaussian
   * @param    amp            Amplitude for Gaussian. Will be the value at the center of
   *                          the resulting raster.
   *
   * @note                    Raster will be TypeInt
   */
  def gaussian(size:Int, cellWidth:Double, sigma:Double, amp:Double) = {
    val extent = Extent(0,0,size*cellWidth,size*cellWidth)
    val rasterExtent = RasterExtent(extent, cellWidth, cellWidth, size, size)
    val output = IntArrayRasterData.empty(rasterExtent.cols, rasterExtent.rows)

    val denom = 2.0*sigma*sigma

    var r = 0
    var c = 0
    while(r < size) {
      c = 0
      while(c < size) {
        val rsqr = (c - size/2)*(c - size/2) + (r - size/2)*(r - size/2)
        val g = (amp * (math.exp(-rsqr / denom))).toInt
        output.set(c,r,g)
        c += 1
      }
      r += 1
    }

    Kernel(Raster(output,rasterExtent))
  }

  /**
   * Creates a Circle kernel. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
   *
   * @param       size           Number of rows in the resulting raster.
   * @param       cellWidth      Cell width of the resutling raster.
   * @param       rad            Radius of the circle.
   *
   * @note                       Raster will be TypeInt 
   */
  def circle(size:Int, cellWidth:Double, rad:Int) = {
    val extent = Extent(0,0,size*cellWidth,size*cellWidth)
    val rasterExtent = RasterExtent(extent, cellWidth, cellWidth, size, size)
    val output = IntArrayRasterData.empty(rasterExtent.cols, rasterExtent.rows)

    val rad2 = rad*rad

    var r = 0
    var c = 0
    while(r < size) {
      while(c < size) {
        output.set(c,r, if (r*r + c*c < rad2) 1 else 0)
        c += 1
      }
      c = 0
      r += 1
    }

    Kernel(Raster(output,rasterExtent))
  }
}
