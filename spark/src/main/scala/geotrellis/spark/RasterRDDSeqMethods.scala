package geotrellis.spark

trait RasterRDDSeqMethods[K] { val rasterRDDs: Traversable[TileRasterRDD[K]] }
