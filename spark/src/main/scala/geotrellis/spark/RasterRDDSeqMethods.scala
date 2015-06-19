package geotrellis.spark

import geotrellis.raster.Tile

trait RasterRDDSeqMethods[K] { val rasterRDDs: Traversable[RasterRDD[K, Tile]] }
