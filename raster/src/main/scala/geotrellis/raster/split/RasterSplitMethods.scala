package geotrellis.raster.split

import geotrellis.raster._

import spire.syntax.cfor._

import Split.Options

abstract class RasterSplitMethods[T <: CellGrid: (? => SplitMethods[T])] extends SplitMethods[Raster[T]] {
  def split(tileLayout: TileLayout, options: Options): Array[Raster[T]] =
    self.rasterExtent.split(tileLayout, options)
      .zip(self.tile.split(tileLayout, options))
      .map { case (re, tile) => Raster(tile, re.extent) }
}
