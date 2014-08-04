package geotrellis.raster

case class MultiBandArrayTile(multiBandData: Array[Tile]) extends MultiBandTile with Serializable {
   
  val cols: Int = multiBandData (0).cols
  val rows: Int = multiBandData (0).rows
  val bands: Int = multiBandData.length
  
  val cellType: CellType = multiBandData (0).cellType
  
  def getBand(bandNo: Int): Tile = multiBandData (bandNo)
  
//  def convert(cellType: CellType): MultiBandTile 
//  def map(f: Int => Int): MultiBandTile
//  def mapDouble(f: Double => Double): MultiBandTile
  
  
}