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

  def combine(firstBandIndex: Int, secondBandIndex: Int)(f: (Int, Int) => Int): Tile

  def combineDouble(firstBandIndex: Int, secondBandIndex: Int)(f: (Double, Double) => Double): Tile

  def dualCombine(firstBandIndex: Int, secondBandIndex: Int)(f: (Int, Int) => Int)(g: (Double, Double) => Double): Tile =
    if (cellType.isFloatingPoint) combineDouble(firstBandIndex, secondBandIndex)(g)
    else combine(firstBandIndex, secondBandIndex)(f)

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
}