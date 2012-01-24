package trellis.operation

import trellis._
import trellis.process._
import trellis.geometry._

/**
 * Used for Kernel Density calculation
 *
 * @see Kernel$
 */
class Kernel(val data: Array[Int])

/**
 * Object used for generating kernels
 */
object Kernel {

  /**
   * Type alias for our gaussian functions. (Double, Double) => Double.
   */
  type Gaussian = Function2[Double, Double, Double]

  /**
   * Creates a kernel
   *
   * @param data Array of integers representing the 2d kernel
   */
  def apply(data: Array[Int]):Kernel = new Kernel(data)

  /**
   * Creates a kernel
   *
   * @param size Size of the kernel (square kernel, so width or height)
   * @param f kernel generating function. If f(x,y) returns negative values
   * they will be ignored
   */
  def apply(size: Int, f:Gaussian):Kernel = {
    val r = Array.ofDim[Int](size*size)
    var row = 0
    var col = 0
    while(row < size) {
      col = 0
      
      while(col < size) {
        val x = col - size/2
        val y = row - size/2
        val idx = row*size + col
        val value = f(x,y)
        r(idx) = if (value >= 0) value.toInt else 0
        col += 1
      }
      
      row += 1
    }
    
    Kernel(r)
  }

  /**
   * Predefined functions useful for generating kernels
   */
  object Function {
    
    //TODO:
    // Triangular [Linear]
    // Exponential [e^x]
    // Quartic

    /**
     * Function that returns a uniform circle
     *
     * @param r: Radius
     * @param a: Amplitude
     */
    def uniform(r: Double, a:Double = 1.0): Gaussian = {
      (x:Double, y:Double) => if (x * x + y * y < r * r) a else 0
    }

    /**
     * A function that returns a 2d guassian
     *
     * @param a: Amplitude
     * @param x0,y0: X/Y shift from center
     * @param sigmaX,sigmaY: 'Spread' factor (std devation)
     *
     * @return function that takes (x,y) and returns guassian value
     */
    def gaussian(a:Double = 1.0, sigmaX:Double = 1.0, sigmaY:Double = 1.0,
                 x0:Double = 0.0, y0:Double = 0.0): Gaussian = {
      (x:Double, y:Double) => {
        val xx = ((x - x0) * (x - x0)) / (2.0 *(sigmaX * sigmaX))
        val yy = ((y - y0) * (y - y0)) / (2.0 *(sigmaY * sigmaY))
        a * math.exp(-(xx + yy))
      }
    }

    /**
     * A function that returns a 2d guassian, parameterized as if in 1d
     *
     * @param a: Amplitude
     * @param sigma: 'Spread' factor (std devation)
     *
     * @return function that takes (x,y) and returns guassian value
     */
    def gaussian1d(a:Double = 1.0, sigma:Double = 1.0):Gaussian = {
      gaussian(a, sigma, sigma, 0.0, 0.0)
    }
  }

}

/**
 * Compute the kernel density of a set of points onto a raster
 *
 * This operation is currently O(n*m) where n is the number of points and m is
 * the size of the kernel array.
 *
 * @param outputRasterExtent: Operation returning the RasterExtent for the
 *   output raster
 * @param kernel: Operation returning a Kernel object to use
 * @param points: Operation returning an array of points to used for the
 *   density calculation
 *
 * Note that the kernel must be square (sqrt(len(kernel)) is an integer)
 * 
 * @see trellis.operation.Kernel$ for methods of creating the input kernel
 */
case class KernelDensity(re:Op[RasterExtent], k:Op[Kernel], pts:Op[Array[Point]])
extends Op[IntRaster] {

  def _run(context:Context) = runAsync(List(re, k, pts))

  val nextSteps:Steps = {
    case (re:RasterExtent) :: (kernel:Kernel) :: (pts:Array[Point]) :: Nil => {
      val raster = IntRaster.createEmpty(re)
      val k = kernel.data
  
      val w = math.sqrt(k.length).toInt
      if (w * w != k.length) {
        sys.error("You must use a square kernel")
      }
  
      var ptIdx = 0
      val ptLen = pts.length
  
      val cols = raster.cols
      val rows = raster.rows
      while(ptIdx < ptLen) {
        val pt = pts(ptIdx)
  
        val px = ((pt.x - re.extent.xmin) / re.cellwidth).toInt
        val py = ((re.extent.ymax - pt.y) / re.cellheight).toInt
  
        val w2 = w / 2
        if ((px > -w2 && px < cols + w2) && (py > -w2 && py < rows + w2)) {
          stampNeigh(raster, pt.value, px, py, w, w, k)
        }
  
        ptIdx += 1
      }
  
      Result(raster)
    }
  }

  private[this] def stampNeigh(raster: IntRaster, v: Int, x: Int, y: Int,
                               dx: Int, dy: Int, kernel: Array[Int]) {
    var k = 0
    val nr = raster.rows
    val nc = raster.cols
    var c:Int = x - dx / 2
    var r:Int = 0

    while(c <= x + dx / 2) {
      r = y - dy / 2

      while(r <= y + dy / 2) {
        if (c >= 0 && c < nc && r >= 0 && r < nr) {
          val i = kernel(k) * v
          // Don't stamp zero values
          if (i > 0) {
            val z = raster.get(c, r)
            raster.set(c, r, if (z == NODATA) i else z + i)
          }
        }
       
        k += 1
        r += 1
      }
      
      c += 1
    }
  }
}
