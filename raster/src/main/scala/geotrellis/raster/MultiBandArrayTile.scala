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

  def combine(other: MultiBandTile)(f: (Int, Int) => Int): MultiBandTile = {
    if (this.bands != other.bands){
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }else if (this.dimensions != other.dimensions){
      throw new Exception("MultiBandTile.dimensions") 
    }else {
      val output = (for (i <- 0 until this.bands) yield { this.getBand(i).combine(other.getBand(i))(f)}).toArray
      MultiBandTile(output)
    }
  }

  def combineDouble(other: MultiBandTile)(f: (Double, Double) => Double): MultiBandTile = {
    if (this.bands != other.bands){
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }else if (this.dimensions != other.dimensions){
      throw new Exception("MultiBandTile.dimensions") 
    }else {
      val output = (for (i <- 0 until this.bands) yield { this.getBand(i).combineDouble(other.getBand(i))(f)}).toArray
      MultiBandTile(output)
    }
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
  
  def combine(first: Int, last: Int)(f: (Int, Int) => Int): Tile = {
    if(bands < 2){
      throw new IndexOutOfBoundsException("MultiBandTile.band")
    }else if(bands <= first || bands <= last){
      throw new IndexOutOfBoundsException("MultiBandTile.band with Index")
    }else{
      getBand(first).combine(getBand(last))(f)
    }
  }

  def combineDouble(first: Int, last: Int)(f: (Double, Double) => Double): Tile = {
    if(bands < 2){
      throw new IndexOutOfBoundsException("MultiBandTile.band")
    }else if(bands <= first || bands <= last){
      throw new IndexOutOfBoundsException("MultiBandTile.band with Index")
    }else{
      getBand(first).combineDouble(getBand(last))(f)
    }
  }
  
  def ndvi(redBandIndex: Int, greenBandIndex: Int): Tile = {
    if(bands < 2)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < 2")
    else if(redBandIndex >= bands || greenBandIndex >= bands)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < (redBandIndex or greenBandIndex)")
    else
      combine(redBandIndex, greenBandIndex)((r,g) => (r-g)/(r+g))
  }
  
  def ndviDouble(redBandIndex: Int, greenBandIndex: Int): Tile = {
    if(bands < 2)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < 2")
    else if(redBandIndex >= bands || greenBandIndex >= bands)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < (redBandIndex or greenBandIndex)")
    else
      combineDouble(redBandIndex, greenBandIndex)((r,g) => (r-g)/(r+g))
  }
  
}
