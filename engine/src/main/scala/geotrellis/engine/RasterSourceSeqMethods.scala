package geotrellis.engine

/** This trait can be extended to add methods to Traversable[RasterSource], such
  * as local operations. To do so, extent RasterSourceSeqMethods, then
  * define an implicit class in the package object that
  * wraps Travesable[RasterSourceSeq], that extends your Methods trait. See
  * [[LocalSeqRasterSourceMethods]] and [[geotrellis.raster.op.local.LocalRasterSourceSeqMethodExtensions]]
  */
trait RasterSourceSeqMethods { val rasterSources: Traversable[RasterSource] }

