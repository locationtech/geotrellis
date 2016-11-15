package geotrellis.raster

import geotrellis.util.MethodExtensions

trait DelayedConversionTileMethods extends MethodExtensions[Tile] {
  /** Delays the conversion of this tile's cell type until
    * it produces another Tile.
    *
    * @param      cellType       The target cell type of a future map or combine operation.
    *
    * @note This only has an affect for the result tiles of a combine or map operation.
    *       This will always produce an ArrayTile.
    */
  def delayedConversion(cellType: CellType): DelayedConversionTile =
    new DelayedConversionTile(self, cellType)
}

trait DelayedConversionMultibandTileMethods extends MethodExtensions[MultibandTile] {
  /** Delays the conversion of this tile's cell type until
    * it produces another Tile or MulitbandTile.
    *
    * @param      cellType       The target cell type of a future map or combine operation.
    *
    * @note This only has an affect for the result tiles of a combine or map operation.
    *       This will always produce an ArrayMultibandTile.
    */
  def delayedConversion(cellType: CellType): DelayedConversionMultibandTile =
    new DelayedConversionMultibandTile(self, cellType)
}
