package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.feature.Point
import geotrellis.feature.op._
import scala.math.{max,min}

/**
 * Creates a Gaussian raster. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
 *
 * @param    size           Number of rows of the resulting raster.
 * @param    cellWidth      Cell width of the resulting raster
 * @param    sigma          Sigma parameter for Gaussian
 * @param    amp            Amplitude for Gaussian. Will be the value at the center of
 *                          the resulting raster.
 *
 * @note                    Raster will be TypeInt
 */
case class CreateGaussianRaster(size: Op[Int], cellWidth: Op[Double], sigma: Op[Double], amp: Op[Double]) 
     extends Op4[Int,Double,Double,Double,Raster](size, cellWidth, sigma, amp)({ (n,w,sigma,amp) => 

       val extent = Extent(0,0,n*w,n*w)
       val rasterExtent = RasterExtent(extent, w, w, n, n)
       val output = IntArrayRasterData.empty(rasterExtent.cols, rasterExtent.rows)

       val denom = 2.0*sigma*sigma

       var r = 0
       var c = 0
       while(r < n) {
         while(c < n) {
           val rsqr = (c - n/2)*(c - n/2) + (r - n/2)*(r - n/2)
           val g = (amp * (math.exp(-rsqr / denom))).toInt
           output.set(c,r,g)

           c += 1
         }
         c = 0
         r += 1
       }

       Result(Raster(output,rasterExtent))
     })

/**
 * Creates a Circle raster. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
 *
 * @param       size           Number of rows in the resulting raster.
 * @param       cellWidth      Cell width of the resutling raster.
 * @param       rad            Radius of the circle.
 *
 * @note                       Raster will be TypeInt 
 */
case class CreateCircleRaster(size: Op[Int], cellWidth: Op[Double], rad: Op[Int]) 
    extends Op3[Int,Double,Int,Raster](size, cellWidth,rad)({ (size, cellWidth, rad) =>

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

    Result(Raster(output,rasterExtent))
  })

/**
 * Computes the convolution of two rasters.
 *
 * @param      r       Raster to convolve.
 * @param      k       Kernel that represents the convolution filter.
 * @param      tns     TileNeighbors that describe the neighboring tiles.
 *
 * @note               Convolve does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Convolve(r:Op[Raster], k:Op[Kernel],tns:Op[TileNeighbors]) extends FocalOp[Raster](r,k,tns)({
  (r,n) => 
    n match {
      case k:Kernel => new ConvolveCalculation(k)
      case _ => sys.error("Convolve must take a Kernel neighborhood.")
    }
})

object Convolve {
 
}

/**
 * Supplies functionaltiy to operations that do convolution.
 */
trait Convolver extends IntRasterDataResult {
  protected var kraster:Raster = null
  protected var rows = 0
  protected var cols = 0
  protected var kernelrows = 0
  protected var kernelcols = 0

  def initKernel(k:Kernel) = {
    kraster = k.raster
    kernelrows = kraster.rows
    kernelcols = kraster.cols
  }

  override def init(r:Raster) = {
    super.init(r)
    rows = r.rows
    cols = r.cols
  }

  def init(re:RasterExtent) = {
    rasterExtent = re
    data = IntArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)
    rows = re.rows
    cols = re.cols

  }

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
}

class ConvolveCalculation(k:Kernel) extends FocalCalculation[Raster] with Convolver {
  initKernel(k)

  def execute(r:Raster,n:Neighborhood,neighbors:Seq[Option[Raster]]):Unit = {
    n match {
      case k:Kernel => execute(r,k,neighbors)
      case _ => sys.error("Convolve operation neighborhood must be of type Kernel")
    }
  }

  def execute(raster:Raster,kernel:Kernel,neighbors:Seq[Option[Raster]]):Unit = {
    val analysisArea = AnalysisArea(raster)
    val r = TileWithNeighbors(raster,neighbors)

    val result = Raster.empty(raster.rasterExtent);
    val data = result.data.mutable.get

    val rowMax = analysisArea.rowMax
    val colMax = analysisArea.colMax

    val kraster = kernel.raster

    val kernelrows = kraster.rows
    val kernelcols = kraster.cols

    var focusRow = analysisArea.rowMin
    var focusCol = analysisArea.colMin

    while(focusRow < rows) {
      focusCol = 0
      while(focusCol < cols) {
        val z = r.get(focusCol,focusRow)

        if (z != NODATA && z != 0) {
          stampKernel(focusCol,focusRow,z)
        }

        focusCol += 1           
      }
      focusRow += 1
    }

    Result(result)    
  }
}
