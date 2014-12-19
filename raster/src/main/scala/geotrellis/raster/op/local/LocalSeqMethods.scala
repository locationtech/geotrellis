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

  def localMin(): Tile =
    Min(tiles)

  def localMinN(n: Int): Tile =
    MinN(n, tiles.toSeq)

  def localMax(): Tile =
    Max(tiles)

  def localMaxN(n: Int): Tile =
    MaxN(n, tiles.toSeq)

  def localMinority(): Tile =
    Minority(tiles.toSeq)

  def localMinority(level: Int): Tile =
    Minority(level, tiles.toSeq)

  def localMajority(): Tile =
    Majority(tiles.toSeq)

  def localMajority(level: Int): Tile =
    Majority(level, tiles.toSeq)

  def localVariance(): Tile = Variance(tiles)
}
