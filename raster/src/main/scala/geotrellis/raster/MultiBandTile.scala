package geotrellis.raster

object MultiBandTile {
  def apply(arr: Array[Tile]): MultiBandTile =
    MultiBandArrayTile(arr)
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
    
}
