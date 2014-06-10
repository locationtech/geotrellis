package geotrellis.raster

/** This trait can be extended to add methods to tile, such
  * as local operations. To do so, extent TileMethods, then
  * define an implicit class in the package object that
  * wraps Tile, that extends your Methods trait. See
  * [[LocalMethods]] and [[geotrellis.raster.op.local.LocalMethodExtensions]]
  */
trait TileMethods { val tile: Tile }

