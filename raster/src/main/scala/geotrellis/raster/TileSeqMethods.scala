package geotrellis.raster

/** This trait can be extended to add methods to Traversable[Tile], such
  * as local operations. To do so, extent TileSeqMethods, then
  * define an implicit class in the package object that
  * wraps Travesable[Tile], that extends your Methods trait. See
  * [[LocalSeqMethods]] and [[geotrellis.raster.op.local.LocalSeqMethodExtensions]]
  */
trait TileSeqMethods { val tiles: Traversable[Tile] }

