package geotrellis.raster

package object mosaic {
  type MergeView[T] = T => MergeMethods[T]

  implicit class withTileMergeMethods(val tile: Tile) extends TileMergeMethods
  implicit class withMultiBandTileMergeMethods(val tile: MultiBandTile) extends MultiBandTileMergeMethods
}
