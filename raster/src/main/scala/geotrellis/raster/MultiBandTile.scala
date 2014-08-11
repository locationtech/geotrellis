package geotrellis.raster

import geotrellis.vector.Extent

object MultiBandTile {
  def apply(arr: Array[Tile]): MultiBandTile =
    MultiBandArrayTile(arr)

  def empty(cellType: CellType, noOfBands: Int, cols: Int, rows: Int): MultiBandTile =
    if (noOfBands < 2) {
      sys.error("There should be at least two Tiles to be MultiBandTile")
    } else {
      val output = Array.ofDim[Tile](noOfBands)
      for (i <- 0 until noOfBands) yield { output(i) = ArrayTile.empty(cellType, cols, rows) }
      MultiBandTile(output)
    }
}

/**
 * Base trait for a MultiBandTile.
 */
trait MultiBandTile {

  val cols: Int
  val rows: Int
  val bands: Int
  lazy val dimensions: (Int, Int) = (cols, rows)
  lazy val sizeOfBand = cols * rows

  val cellType: CellType

  def getBand(bandNo: Int): Tile

  def map(f: Int => Int): MultiBandTile
  def mapDouble(f: Double => Double): MultiBandTile

  def dualMap(f: Int => Int)(g: Double => Double): MultiBandTile =
    if (cellType.isFloatingPoint) mapDouble(g)
    else map(f)

  def convert(cellType: CellType): MultiBandTile

  def combine(other: MultiBandTile)(f: (Int, Int) => Int): MultiBandTile
  def combineDouble(other: MultiBandTile)(f: (Double, Double) => Double): MultiBandTile

  def dualCombine(other: MultiBandTile)(f: (Int, Int) => Int)(g: (Double, Double) => Double): MultiBandTile =
    if (cellType.isFloatingPoint) combineDouble(other)(g)
    else combine(other)(f)

  def mapIfSet(f: Int => Int): MultiBandTile =
    map { i =>
      if (isNoData(i)) i
      else f(i)
    }

  def mapIfSetDouble(f: Double => Double): MultiBandTile =
    mapDouble { d =>
      if (isNoData(d)) d
      else f(d)
    }

  def dualMapIfSet(f: Int => Int)(g: Double => Double): MultiBandTile =
    if (cellType.isFloatingPoint) mapIfSetDouble(g)
    else mapIfSet(f)

  def combine(first: Int, last: Int)(f: (Int, Int) => Int): Tile
  def combineDouble(first: Int, last: Int)(f: (Double, Double) => Double): Tile

  def dualCombine(first: Int, last: Int)(f: (Int, Int) => Int)(g: (Double, Double) => Double): Tile =
    if (cellType.isFloatingPoint) combineDouble(first, last)(g)
    else combine(first, last)(f)

  def warp(source: Extent, target: RasterExtent): MultiBandTile
  def warp(source: Extent, target: Extent): MultiBandTile
  def warp(source: Extent, targetCols: Int, targetRows: Int): MultiBandTile

  /**
   * implementation of Local sequence methods on single MultiBandTile
   */
  def min(first: Int, last: Int): Tile
  def minDouble(first: Int, last: Int): Tile
  def dualMin(first: Int, last: Int): Tile

  def max(first: Int, last: Int): Tile
  def maxDouble(first: Int, last: Int): Tile
  def dualMax(first: Int, last: Int): Tile

  def mean(first: Int, last: Int): Tile
  def meanDouble(first: Int, last: Int): Tile
  def dualMean(first: Int, last: Int): Tile

  def ndvi(redBandIndex: Int, greenBandIndex: Int): Tile
  def ndviDouble(redBandIndex: Int, greenBandIndex: Int): Tile

  def dualndvi(redBandIndex: Int, greenBandIndex: Int): Tile = {
    if (cellType.isFloatingPoint) ndviDouble(redBandIndex, greenBandIndex)
    else ndvi(redBandIndex, greenBandIndex)
  }

  /**
   * Implementation of Local specific operations on single MultiBandTile
   */
  def localAdd(constant: Int): MultiBandTile
  def localAdd(): Tile
  def localSubtract(constant: Int): MultiBandTile
  def localSubtract(): Tile
  def localMultiply(constant: Int): MultiBandTile
  def localMultiply(): Tile
  def localDivide(constnt: Int): MultiBandTile
  def localDivide(): Tile
}
