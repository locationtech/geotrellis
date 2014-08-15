package geotrellis.raster.multiband

import geotrellis.vector.Extent
import spire.syntax.cfor._
import geotrellis.raster.CellType
import geotrellis.raster.RasterExtent
import geotrellis.raster.Tile
import geotrellis.raster._

case class MultiBandArrayTile(multiBandData: Array[Tile]) extends MultiBandTile with Serializable {

  /**
   * check whether there is at least two bands
   */
  if (multiBandData.length < 2)
    sys.error("There should be at least two Tiles to be MultiBandTile")

  val cols: Int = multiBandData(0).cols
  val rows: Int = multiBandData(0).rows
  val bands: Int = multiBandData.length

  val cellType: CellType = multiBandData(0).cellType

  /**
   * check whether each Tile in multiBandData
   *  has similar CellType, Rows and Cols
   */
  cfor(0)(_ < bands, _ + 1) { band =>
    if (multiBandData(band).cols != cols || multiBandData(band).rows != rows || multiBandData(band).cellType != cellType)
      sys.error("All band should have same cols, rows and Type, Band at Index $band is differ")
  }

  def getBand(bandNo: Int): Tile = {
    if (bandNo < bands)
      multiBandData(bandNo)
    else
      throw new IndexOutOfBoundsException("MultiBandTile.band")
  }

  def map(f: Int => Int): MultiBandTile = {
    val outputData = Array.ofDim[Tile](bands)
    cfor(0)(_ < bands, _ + 1) { band =>
      outputData(band) = getBand(band).map(f)
    }
    MultiBandTile(outputData)
  }

  def mapDouble(f: Double => Double): MultiBandTile = {
    val outputData = Array.ofDim[Tile](bands)
    cfor(0)(_ < bands, _ + 1) { band =>
      outputData(band) = getBand(band).mapDouble(f)
    }
    MultiBandTile(outputData)
  }

  def convert(cellType: CellType): MultiBandTile = {
    val outputData = Array.ofDim[Tile](bands)
    cfor(0)(_ < bands, _ + 1) { band =>
      outputData(band) = getBand(band).convert(cellType)
    }
    MultiBandTile(outputData)
  }

  /**
   * combine two multibandtiles according to given function
   */
  def combine(other: MultiBandTile)(f: (Int, Int) => Int): MultiBandTile = {
    if (this.bands != other.bands) {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    } else if (this.dimensions != other.dimensions) {
      throw new Exception("MultiBandTile dimensions of bands are not Equal")
    } else {
      val output = Array.ofDim[Tile](bands)
      cfor(0)(_ < bands, _ + 1) { band =>
        output(band) = getBand(band).combine(other.getBand(band))(f)
      }
      MultiBandTile(output)
    }
  }

  def combineDouble(other: MultiBandTile)(f: (Double, Double) => Double): MultiBandTile = {
    if (this.bands != other.bands) {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    } else if (this.dimensions != other.dimensions) {
      throw new Exception("MultiBandTile dimensions of bands are not Equal")
    } else {
      val output = Array.ofDim[Tile](bands)
      cfor(0)(_ < bands, _ + 1) { band =>
        output(band) = getBand(band).combineDouble(other.getBand(band))(f)
      }
      MultiBandTile(output)
    }
  }

  /**
   *  combine bands in a single multibandtile according to given function
   */
  def combine(first: Int, last: Int)(f: (Int, Int) => Int): Tile = {
    if (first < bands || last < bands) {
      var result = getBand(first)
      cfor(first + 1)(_ < last, _ + 1) { band =>
        result = result.combine(getBand(band))(f)
      }
      result
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def combineDouble(first: Int, last: Int)(f: (Double, Double) => Double): Tile = {
    if (first < bands || last < bands) {
      var result = getBand(first)
      cfor(first + 1)(_ < last, _ + 1) { band =>
        result = result.combineDouble(getBand(band))(f)
      }
      result
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def warp(source: Extent, target: RasterExtent): MultiBandTile = {
    val outPutData = Array.ofDim[Tile](bands)
    cfor(0)(_ < bands, _ + 1) { band =>
      outPutData(band) = getBand(band).warp(source, target)
    }
    MultiBandTile(outPutData)
  }

  def warp(source: Extent, target: Extent): MultiBandTile = {
    val outPutData = Array.ofDim[Tile](bands)
    cfor(0)(_ < bands, _ + 1) { band =>
      outPutData(band) = getBand(band).warp(source, target)
    }
    MultiBandTile(outPutData)
  }

  def warp(source: Extent, targetCols: Int, targetRows: Int): MultiBandTile = {
    val outPutData = Array.ofDim[Tile](bands)
    cfor(0)(_ < bands, _ + 1) { band =>
      outPutData(band) = getBand(band).warp(source, targetCols, targetRows)
    }
    MultiBandTile(outPutData)
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

  /**
   * implementation of Local sequence methods on single MultiBandTile
   */

  def min(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      combine(first, last)((i, j) => math.min(i, j))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def minDouble(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      combineDouble(first, last)((i, j) => math.min(i, j))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def dualMin(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      dualCombine(first, last)((i, j) => math.min(i, j))((i, j) => math.min(i, j))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def max(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      combine(first, last)((i, j) => math.max(i, j))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def maxDouble(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      combineDouble(first, last)((i, j) => math.max(i, j))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def dualMax(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      dualCombine(first, last)((i, j) => math.max(i, j))((i, j) => math.max(i, j))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def mean(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      combine(first, last)((i, j) => i + j).map(k => k / (last - first + 1))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def meanDouble(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      combineDouble(first, last)((i, j) => i + j).map(k => k / (last - first + 1))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def dualMean(first: Int, last: Int): Tile = {
    if (first < bands || last < bands) {
      dualCombine(first, last)((i, j) => i + j)((i, j) => i + j).map(k => k / (last - first + 1))
    } else {
      throw new IndexOutOfBoundsException("MultiBandTile.bands")
    }
  }

  def ndvi(redBandIndex: Int, greenBandIndex: Int): Tile = {
    if (redBandIndex >= bands || greenBandIndex >= bands)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < (redBandIndex or greenBandIndex)")
    else
      combine(redBandIndex, greenBandIndex)((r, g) => (r - g) / (r + g))
  }

  def ndviDouble(redBandIndex: Int, greenBandIndex: Int): Tile = {
    if (redBandIndex >= bands || greenBandIndex >= bands)
      throw new IndexOutOfBoundsException("MultiBandTile.bands < (redBandIndex or greenBandIndex)")
    else
      combineDouble(redBandIndex, greenBandIndex)((r, g) => (r - g) / (r + g))
  }

  /**
   * Implementation of Local specific operations on single MultiBandTile
   */
  def localAdd(constant: Int): MultiBandTile = {
    this.map(a => a + constant)
  }

  def localAdd(): Tile = {
    this.combine(0, this.bands - 1)((a, b) => a + b)
  }

  def localSubtract(constant: Int): MultiBandTile = {
    this.map(a => a - constant)
  }

  def localSubtract(): Tile = {
    this.combine(0, this.bands - 1)((a, b) => a - b)
  }

  def localMultiply(constant: Int): MultiBandTile = {
    this.map(a => a * constant)
  }

  def localMultiply(): Tile = {
    this.combine(0, this.bands - 1)((a, b) => a * b)
  }

  def localDivide(constnt: Int): MultiBandTile = {
    this.map(a => a / constnt)
  }

  def localDivide(): Tile = {
    this.combine(0, this.bands - 1)((a, b) => a / b)
  }

  def localDefined(): MultiBandTile = {
    this.map(a => if (isNoData(a)) 0 else 1)
  }

  def localUndefined(): MultiBandTile = {
    this.map(a => if (isNoData(a)) 1 else 0)
  }

  def localPow(constant: Int): MultiBandTile = {
    this.map(a => math.pow(a.toDouble, constant.toDouble).toInt)
  }

  def localSqrt(): MultiBandTile = {
    this.map(a => math.sqrt(a.toDouble).toInt)
  }
  
}
