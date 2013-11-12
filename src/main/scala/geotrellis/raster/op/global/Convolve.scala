package geotrellis.raster.op.global

import geotrellis._
import geotrellis.raster._
import geotrellis.feature.Point
import geotrellis.feature.op._

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
case class Convolve(r:Op[Raster], k:Op[Kernel]) extends Op2(r,k)({
  (r,kernel) => 
    val convolver = new Convolver(r.rasterExtent,kernel)

    val cols = r.rasterExtent.cols
    val rows = r.rasterExtent.rows

    val kraster = kernel.raster

    val kernelrows = kraster.rows
    val kernelcols = kraster.cols

    var col = 0
    var row = 0

    while(row < rows) {
      col = 0
      while(col < cols) {
        val z = r.get(col,row)
        if (z != NODATA) {
          convolver.stampKernel(col,row,z)
        }

        col += 1
      }
      row += 1
    }

    Result(convolver.result)
})
