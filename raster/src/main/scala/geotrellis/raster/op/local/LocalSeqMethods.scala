package geotrellis.raster.op.local

import geotrellis.raster._

trait LocalSeqMethods extends TileSeqMethods {
  def localAdd(): Tile = Add(tiles)

 /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety(): Tile =
    Variety(tiles)

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(): Tile =
    Mean(tiles)
}
