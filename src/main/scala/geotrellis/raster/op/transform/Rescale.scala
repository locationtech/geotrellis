package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.data._

/**
 * Rescale the size of a raster by a fixed percentage, maintaining the aspect ratio.
 * 
 * This operation uses a nearest neighbor algorithm to resize a raster by a 
 * given percentage.
 * 
 * For example, if the rescale percentage is 2.0, the number of columns and
 * the number of rows will be doubled.  If the original raster had 100 columns 
 * and 100 rows and each cell had a cell width and a cell height in map units 
 * of 50m, the new raster would be 200x200 (cols and rows) and the new cell width 
 * and height would be 25m.
 * 
 * @param rescalePct  Fraction of input raster size.
 *
 * @note               Rescale does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Rescale(r:Op[Raster], rescalePct:Op[Double]) extends Op2(r,rescalePct) ({
  (r,rescalePct) => {
    val re = r.rasterExtent
    val cw = re.cellwidth / rescalePct
    val ch = re.cellheight / rescalePct
    val newRasterExtent = re.withResolution(cw, ch)
    Result(RasterReader.read(r, Option(newRasterExtent)))
  }
})

