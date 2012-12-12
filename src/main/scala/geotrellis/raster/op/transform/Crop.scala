package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.raster._

/**
 * Operation to crop a raster.
 */
case class Crop(r:Op[Raster], extent:Extent) 
     extends Op2(r,extent)((r,extent) => Result(CroppedRaster(r,extent))) { }
