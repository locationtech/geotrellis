package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

/**
  * This is a conceptual port of GDALSuggestedWarpOutput2, part of GDAL. Docstring paraphrased
  * from that code.
  */
object ReprojectRasterExtent {
  /*
   * This function is used to suggest the extent and resolution
   * appropriate given the indicated reprojection CRSs.  It walks
   * the edges of the input file (approximately 20 sample points along each
   * edge) transforming into output coordinates in order to get an extents box.
   */
  def reprojectExtent(re: RasterExtent, transform: Transform): Extent = {
    val PIXEL_STEP = 20

    val Extent(xmin, ymin, xmax, ymax) = re.extent
    val cellwidth = re.cellwidth
    val cellheight = re.cellheight

    val xlen = ((xmax - xmin) / cellwidth).toInt + 1
    val ylen = ((ymax - ymin) / cellheight).toInt + 1

    var (currentXmin, currentYmin) = transform(xmin, ymin)
    var currentXmax = currentXmin
    var currentYmax = currentYmin
    
    var currentX = xmin + cellwidth
    var currentY = ymin + cellheight
    cfor(1)(_ < math.max(xlen,ylen), _ + 1) { i =>
      if(i < xlen) {
        val (x1, y1) = transform(currentX, ymax)
        val (x2, y2) = transform(currentX, ymin)
        if(x1 < x2) {
          if(x1 < currentXmin) 
            currentXmin = x1
          if(currentXmax < x2)
            currentXmax = x2
        } else {
          if(x2 < currentXmin) 
            currentXmin = x2
          if(currentXmax < x1)
            currentXmax = x1
        }
      }

      if(i < ylen) {
        val (x1, y1) = transform(xmax, currentY)
        val (x2, y2) = transform(xmin, currentY)
        if(x1 < x2) {
          if(x1 < currentXmin) 
            currentXmin = x1
          if(currentXmax < x2)
            currentXmax = x2
        } else {
          if(x2 < currentXmin) 
            currentXmin = x2
          if(currentXmax < x1)
            currentXmax = x1
        }
      }
    }
    
    Extent(currentXmin, currentYmin, currentXmax, currentYmax)
  }

  /* Aa resolution is computed with the intent that the length of the
   * distance from the top left corner of the output imagery to the bottom right
   * corner would represent the same number of pixels as in the source image. 
   * Note that if the image is somewhat rotated the diagonal taken isnt of the
   * whole output bounding rectangle, but instead of the locations where the
   * top/left and bottom/right corners transform.  The output pixel size is 
   * always square.  This is intended to approximately preserve the resolution
   * of the input data in the output file. 
   */
  def apply(re: RasterExtent, transform: Transform): RasterExtent = {
    val extent = re.extent
    val newExtent = reprojectExtent(re, transform)

    val distance = math.sqrt(extent.width*extent.width + extent.height*extent.height)
    val pixelSize = distance / math.sqrt(re.cols * re.cols + re.rows * re.rows)
    val newCols = (newExtent.width / pixelSize).toInt
    val newRows = (newExtent.height / pixelSize).toInt

    RasterExtent(newExtent, newCols, newRows)
  }
}
