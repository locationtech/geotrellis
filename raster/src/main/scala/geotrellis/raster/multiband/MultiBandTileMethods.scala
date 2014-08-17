package geotrellis.raster.multiband

/**
 * This trait can be extended to add methods to MultiBandTile, such
 * as local operations. To do so, extent MultiBandTileMethods, then
 * define an implicit class in the package object that
 * wraps MultiBandTile, that extends your Methods trait. See
 * [[geotrellis.raster.op.local.LocalMethods]]
 *
 * @author kelum
 */
trait MultiBandTileMethods {
  val mTile: MultiBandTile
}