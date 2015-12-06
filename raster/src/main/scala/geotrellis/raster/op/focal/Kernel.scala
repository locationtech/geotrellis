package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.vector.Extent

/** 
 * Kernel
 *
 * Represents a neighborhood that is represented by
 * a tile.
 */
case class Kernel(tile: Tile) extends Neighborhood {
  if(tile.rows != tile.cols) sys.error("Kernel tile must be square")
  if(tile.rows % 2 != 1) sys.error("Kernel tile must have odd dimension")
  val extent = (tile.rows / 2).toInt

  // Not supporting masks, since masks are implemented as 0 values in the kernel weight
  val hasMask = false
}

object Kernel {
  implicit def tile2Kernel(r: Tile): Kernel = Kernel(r)
  
  /**
   * Creates a Gaussian kernel. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
   *
   * @param    size           Number of rows of the resulting tile.
   * @param    sigma          Sigma parameter for Gaussian
   * @param    amp            Amplitude for Gaussian. Will be the value at the center of
   *                          the resulting tile.
   *
   * @note                    Tile will be TypeInt
   */
  def gaussian(size: Int, sigma: Double, amp: Double): Kernel = {
    val output = IntArrayTile.empty(size, size)

    val denom = 2.0*sigma*sigma

    var r = 0
    var c = 0
    while(r < size) {
      c = 0
      while(c < size) {
        val rsqr = (c - (size / 2)) * (c - (size / 2)) + (r - (size / 2)) * (r - (size / 2))
        val g = (amp * (math.exp(-rsqr / denom))).toInt
        output.set(c, r, g)
        c += 1
      }
      r += 1
    }

    Kernel(output)
  }

  /**
   * Creates a Circle kernel. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
   *
   * @param       size           Number of rows in the resulting tile.
   * @param       cellWidth      Cell width of the resutling tile.
   * @param       rad            Radius of the circle.
   *
   * @note                       Tile will be TypeInt 
   */
  def circle(size: Int, cellWidth: Double, rad: Int) = {
    val output = IntArrayTile.empty(size, size)

    val rad2 = rad*rad

    var r = 0
    var c = 0
    while(r < size) {
      while(c < size) {
        output.set(c, r, if (r * r + c * c < rad2) 1 else 0)
        c += 1
      }
      c = 0
      r += 1
    }

    Kernel(output)
  }
}
