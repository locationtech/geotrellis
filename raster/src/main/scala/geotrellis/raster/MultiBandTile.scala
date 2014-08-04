package geotrellis.raster

object MultiBandTile{
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
  
//  def convert(cellType: CellType): MultiBandTile
//  def map(f: Int => Int): MultiBandTile
//  def mapDouble(f: Double => Double): MultiBandTile
}