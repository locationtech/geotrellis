package geotrellis.raster

package object mosaic {
  implicit class withTileMergeMethods(val tile: Tile) extends TileMergeMethods
  implicit class withMultiBandTileMergeMethods(val tile: MultiBandTile) extends MultiBandTileMergeMethods
}
