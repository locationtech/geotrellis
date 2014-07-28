package geotrellis.engine

/** This trait can be extended to add methods to RasterSource, such
  * as local operations. To do so, extent RasterSourceMethods, then
  * define an implicit class in the package object that
  * wraps RasterSource, that extends your Methods trait. See
  * [[geotrellis.engine.op.local.LocalRasterSourceMethods]]
  */
trait RasterSourceMethods {
  val rasterSource: RasterSource
}
