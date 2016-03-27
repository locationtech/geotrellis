package geotrellis.raster.mapalgebra.local

import geotrellis.raster._
import geotrellis.util.MethodExtensions

trait LocalSeqMethods extends MethodExtensions[Traversable[Tile]] {
  def localAdd(): Tile =
    Add(self)
  def +(): Tile = localAdd()

  def localSubtract(): Tile =
    Subtract(self)
  def -(): Tile = localSubtract()

  def localMultiply(): Tile =
    Multiply(self)
  def *(): Tile = localMultiply()

  def localDivide(): Tile =
    Divide(self)

  def localPow(): Tile =
    Pow(self.toSeq)
  def **(): Tile = localPow()

 /** Gives the count of unique values at each location in a set of Self.*/
  def localVariety(): Tile =
    Variety(self)

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(): Tile =
    Mean(self)

  def localMin(): Tile =
    Min(self)

  def localMinN(n: Int): Tile =
    MinN(n, self.toSeq)

  def localMax(): Tile =
    Max(self)

  def localMaxN(n: Int): Tile =
    MaxN(n, self.toSeq)

  def localMinority(): Tile =
    Minority(self.toSeq)

  def localMinority(level: Int): Tile =
    Minority(level, self.toSeq)

  def localMajority(): Tile =
    Majority(self.toSeq)

  def localMajority(level: Int): Tile =
    Majority(level, self.toSeq)

  def localVariance(): Tile =
    Variance(self)

  def localAnd(): Tile =
    And(self)

  def localOr(): Tile =
    Or(self.toSeq)
  def |(): Tile = localOr()

  def localXor(): Tile =
    Xor(self.toSeq)
  def ^(): Tile = localXor()
}
