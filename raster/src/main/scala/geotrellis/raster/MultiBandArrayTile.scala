package geotrellis.raster

case class MultiBandArrayTile(multiBandData: Array[Tile]) extends MultiBandTile with Serializable {

  val cols: Int = multiBandData(0).cols
  val rows: Int = multiBandData(0).rows
  val bands: Int = multiBandData.length

  val cellType: CellType = multiBandData(0).cellType

  /**
   * check whether each Tile in multiBandData
   *  has similar CellType, Rows and Cols
   */
  for (i <- 1 until bands if (multiBandData(i).cols != cols || multiBandData(i).rows != rows || multiBandData(i).cellType != cellType)) yield { sys.error("All band should have same cols, rows and Type, Band at Index $i is differ") }

  def getBand(bandNo: Int): Tile = {
    if (bandNo < bands)
      multiBandData(bandNo)
    else
      throw new IndexOutOfBoundsException("MultiBandTile.band")
  }

  def map(f: Int => Int): MultiBandTile = {
    val outputData = (for (i <- 0 until bands) yield { this.getBand(i).map(f) }).toArray
    MultiBandTile(outputData)
  }

  def mapDouble(f: Double => Double): MultiBandTile = {
    val outputData = (for (i <- 0 until bands) yield { this.getBand(i).mapDouble(f) }).toArray
    MultiBandTile(outputData)
  }

  def convert(cellType: CellType): MultiBandTile = {
    val outputData = (for (i <- 0 until bands) yield { this.getBand(i).convert(cellType) }).toArray
    MultiBandTile(outputData)
  }

  def combine(firstBandIndex: Int, secondBandIndex: Int)(f: (Int, Int) => Int): Tile = {
    if (bands < 2)
      throw new IndexOutOfBoundsException("MultiBandTile.length < 2")
    else if (firstBandIndex >= bands || secondBandIndex >= bands)
      throw new IndexOutOfBoundsException("MultiBandTile.length < (firstBandIndex or secondBandIndex)")
    else
      this.getBand(firstBandIndex).combine(this.getBand(secondBandIndex))(f)
  }

  def combineDouble(firstBandIndex: Int, secondBandIndex: Int)(f: (Double, Double) => Double): Tile = {
    if (bands < 2)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < 2")
    else if (firstBandIndex >= bands || secondBandIndex >= bands)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < (firstBandIndex or secondBandIndex)")
    else
      this.getBand(firstBandIndex).combineDouble(this.getBand(secondBandIndex))(f)
  }

  override def equals(other: Any): Boolean = other match {
    case r: MultiBandArrayTile => {
      if (r == null) return false
      val len = bands
      if (len != r.bands) return false
      var i = 0
      while (i < len) {
        if (this.getBand(i) != r.getBand(i)) return false
        i += 1
      }
      true
    }
    case _ => false
  }

}