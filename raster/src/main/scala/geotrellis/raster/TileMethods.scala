package geotrellis.raster

/** This trait can be extended to add methods to Tile, such
  * as local operations. To do so, extent TileMethods, then
  * define an implicit class in the package object that
  * wraps Tile, that extends your Methods trait. See
  * [[geotrellis.raster.op.local.LocalMethods]]
  */
trait TileMethods extends MethodExtensions[Tile] { 
  val tile: Tile
  val self = tile
}
