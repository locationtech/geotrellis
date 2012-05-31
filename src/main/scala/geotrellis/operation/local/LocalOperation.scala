package geotrellis.operation

import geotrellis._

/**
 * Local operations involve each individual value in a raster without information
 * about other values in the raster. 
 */
trait LocalOperation extends Op[Raster] {}
