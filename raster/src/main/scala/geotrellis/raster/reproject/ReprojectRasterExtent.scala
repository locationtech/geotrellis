package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.proj4._

import com.vividsolutions.jts.densify.Densifier

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
    val PIXEL_STEP = math.min(50, math.min(re.cols, re.rows))
    
    // Find the threshold to densify the extent at.
    val xThreshold = (re.cols / PIXEL_STEP) * re.cellwidth
    val yThreshold = (re.rows / PIXEL_STEP) * re.cellheight
    val threshold = math.min(xThreshold, yThreshold)

    // Densify the extent to get a more accurate reprojection
    val denseGeom = Polygon(Densifier.densify(re.extent.toPolygon.jtsGeom, threshold).asInstanceOf[com.vividsolutions.jts.geom.Polygon])
    denseGeom.reproject(transform).envelope
  }

  /* A resolution is computed with the intent that the length of the
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

    val (transformedXmin, transformedYmax) = {
      transform(extent.xmin, extent.ymax)
    }

    val (transformedXmax, transformedYmin) = {
      transform(extent.xmax, extent.ymin)
    }

    val distance = (transformedXmin, transformedYmax).distance((transformedXmax, transformedYmin))
    val pixelSize = distance / math.sqrt(re.cols * re.cols + re.rows * re.rows)

    val newColsDouble = newExtent.width / pixelSize
    val newRowsDouble = newExtent.height / pixelSize

    val newCols = (newColsDouble + 0.5).toInt
    val newRows = (newRowsDouble + 0.5).toInt

    //Adjust the extent to match the pixel size.
    val adjustedExtent = Extent(newExtent.xmin, newExtent.ymax - (pixelSize*newRows), newExtent.xmin + (pixelSize*newCols), newExtent.ymax)

    RasterExtent(adjustedExtent, pixelSize, pixelSize, newCols, newRows)
  }

  def apply(re: RasterExtent, src: CRS, dest: CRS): RasterExtent =
    apply(re, Transform(src, dest))

}
