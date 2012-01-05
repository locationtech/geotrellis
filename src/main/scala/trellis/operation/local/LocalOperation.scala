package trellis.operation

import trellis.raster.IntRaster

/*
 * Local operations involve each individual value in a raster without information
 * about other values in the raster. 
 */
trait LocalOperation extends Op[IntRaster]
