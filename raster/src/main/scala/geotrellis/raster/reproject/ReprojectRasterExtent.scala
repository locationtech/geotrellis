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
  import Reproject.Options

  /*
   * This function is used to suggest the extent and resolution
   * appropriate given the indicated reprojection CRSs.  It walks
   * the edges of the input file (approximately 20 sample points along each
   * edge) transforming into output coordinates in order to get an extents box.
   */
  def reprojectExtent(gd: GridDefinition, transform: Transform): Extent = {
    val extent = gd.extent
    val (cols, rows) = (extent.width / gd.cellwidth, extent.height / gd.cellheight)
    val PIXEL_STEP = math.min(50.0, math.min(cols, rows)).toInt
    
    // Find the threshold to densify the extent at.
    val xThreshold = (cols / PIXEL_STEP) * gd.cellwidth
    val yThreshold = (rows / PIXEL_STEP) * gd.cellheight
    val threshold = math.min(xThreshold, yThreshold)

    // Densify the extent to get a more accurate reprojection
    val denseGeom = Polygon(Densifier.densify(extent.toPolygon.jtsGeom, threshold).asInstanceOf[com.vividsolutions.jts.geom.Polygon])
    denseGeom.reproject(transform).envelope
  }

  def apply(ge: GridExtent, transform: Transform, options: Options): GridExtent = {
    val extent = ge.extent
    val newExtent = reprojectExtent(ge, transform)

    val (transformedXmin, transformedYmax) = {
      transform(extent.xmin, extent.ymax)
    }

    val (transformedXmax, transformedYmin) = {
      transform(extent.xmax, extent.ymin)
    }

    options.parentGridExtent match {
      case Some(parentGridExtent) =>
        parentGridExtent.createAlignedGridExtent(Extent(transformedXmin, transformedYmin, transformedXmax, transformedYmax))
      case None =>
        val (pixelSizeX, pixelSizeY) =
          options.targetCellSize match {
            case Some(cellSize) =>
              (cellSize.width, cellSize.height)
            case None =>
              val distance = (transformedXmin, transformedYmax).distance((transformedXmax, transformedYmin))
              val cols = ge.extent.width / ge.cellwidth
              val rows = ge.extent.height / ge.cellheight
              val pixelSize = distance / math.sqrt(cols * cols + rows * rows)
              (pixelSize, pixelSize)
          }

        val newColsDouble = newExtent.width / pixelSizeX
        val newRowsDouble = newExtent.height / pixelSizeY

        val newCols = (newColsDouble + 0.5).toInt//.toLong
        val newRows = (newRowsDouble + 0.5).toInt//.toLong

        //Adjust the extent to match the pixel size.
        val adjustedExtent = Extent(newExtent.xmin, newExtent.ymax - (pixelSizeY*newRows), newExtent.xmin + (pixelSizeX*newCols), newExtent.ymax)
        GridExtent(adjustedExtent, pixelSizeX, pixelSizeY)
    }
  }

  def apply(ge: GridExtent, transform: Transform): GridExtent =
    apply(ge, transform, Options.DEFAULT)

  def apply(ge: GridExtent, src: CRS, dest: CRS, options: Options): GridExtent =
    apply(ge, Transform(src, dest), options)

  def apply(ge: GridExtent, src: CRS, dest: CRS): GridExtent =
    apply(ge, src, dest, Options.DEFAULT)

  /* A resolution is computed with the intent that the length of the
   * distance from the top left corner of the output imagery to the bottom right
   * corner would represent the same number of pixels as in the source image.
   * Note that if the image is somewhat rotated the diagonal taken isnt of the
   * whole output bounding rectangle, but instead of the locations where the
   * top/left and bottom/right corners transform.  The output pixel size is
   * always square.  This is intended to approximately preserve the resolution
   * of the input data in the output file.
   */
  def apply(re: RasterExtent, transform: Transform, options: Reproject.Options): RasterExtent =
    apply(re.toGridExtent, transform, options).toRasterExtent

  def apply(re: RasterExtent, transform: Transform): RasterExtent =
    apply(re, transform, Options.DEFAULT)

  def apply(re: RasterExtent, src: CRS, dest: CRS, options: Options): RasterExtent =
    apply(re, Transform(src, dest), options)

  def apply(re: RasterExtent, src: CRS, dest: CRS): RasterExtent =
    apply(re, src, dest, Options.DEFAULT)

}
