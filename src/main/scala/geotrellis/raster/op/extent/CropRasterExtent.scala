package geotrellis.raster.op.extent

import geotrellis._
import scala.math.ceil
import scala.math.floor

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class CropRasterExtent(r:Op[RasterExtent], e:Op[Extent]) 
     extends Op2(r,e)({
  (re, extent) => 
    val xmin = extent.xmin
    val xmax = extent.xmax
    val ymin = extent.ymin
    val ymax = extent.ymax

     // first we figure out which part of the current raster is desired
     val col1 = floor((xmin - re.extent.xmin) / re.cellwidth).toInt
     val row1 = floor((ymin - re.extent.ymin) / re.cellheight).toInt
     val col2 = ceil((xmax - re.extent.xmin) / re.cellwidth).toInt
     val row2 = ceil((ymax - re.extent.ymin) / re.cellheight).toInt

     // then translate back into our coordinates
     val xmin3 = re.extent.xmin + col1 * re.cellwidth
     val xmax3 = re.extent.xmin + col2 * re.cellwidth

     val ymin3 = re.extent.ymin + row1 * re.cellheight
     val ymax3 = re.extent.ymin + row2 * re.cellheight
     val cols3 = col2 - col1
     val rows3 = row2 - row1

     val newExtent = Extent(xmin3, ymin3, xmax3, ymax3)
     Result(RasterExtent(newExtent, re.cellwidth, re.cellheight, cols3, rows3))
})

object CropRasterExtent {
  def apply(re:Op[RasterExtent], xmin:Double, ymin:Double, xmax:Double, ymax:Double):CropRasterExtent = {
    val e = Extent(xmin, ymin, xmax, ymax)
    CropRasterExtent(re, Literal(e))
  }
}
