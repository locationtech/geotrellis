package geotrellis.raster.split

import geotrellis.raster._

import spire.syntax.cfor._

import Split.Options

trait MultibandTileSplitMethods extends SplitMethods[MultibandTile] {
  def split(tileLayout: TileLayout, options: Options): Array[MultibandTile] =
    (0 until self.bandCount)
      .map { b => self.band(b).split(tileLayout, options) }
      .transpose
      .map(ArrayMultibandTile(_))
      .toArray
}
