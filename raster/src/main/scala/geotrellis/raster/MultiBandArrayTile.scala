package geotrellis.raster

case class MultiBandArrayTile(multiBandData: Array[Tile]) extends MultiBandTile with Serializable {
  val data = multiBandData 
  val cols: Int = data(0).cols
  val rows: Int = data(0).rows
  val bands: Int = data.length
//  lazy val dimensions: (Int, Int) = (cols, rows)
//  lazy val sizeOfBand = cols * rows
  
  val cellType: CellType = data(0).cellType
  
  def getBand(bandNo: Int): Tile = data(bandNo)
  
//  def convert(cellType: CellType): MultiBandTile 
//  def map(f: Int => Int): MultiBandTile
//  def mapDouble(f: Double => Double): MultiBandTile
  
  
}