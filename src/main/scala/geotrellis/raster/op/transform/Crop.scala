package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.raster._

/**
 * Create a new raster from the data in a sub-extent of an existing raster.
 * 
 * @param      r         Raster to crop
 * @param      extent    Subextent of r that will be the resulting cropped raster's extent.
 */
case class Crop(r:Op[Raster], extent:Extent) 
     extends Op2(r,extent)((r,extent) => Result(CroppedRaster(r,extent))) { }
