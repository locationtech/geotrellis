package geotrellis.raster.op.local

import geotrellis._
import geotrellis._

/**
 * Local operations involve each individual value in a raster without information
 * about other values in the raster. 
 */
trait LocalOperation extends Op[Raster] {}
