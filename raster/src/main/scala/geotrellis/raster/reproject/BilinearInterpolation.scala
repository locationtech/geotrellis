package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

class BilinearInterpolation(tile: Tile, extent: Extent) extends Interpolation {
  private val re = RasterExtent(tile, extent)
  private val cols = tile.cols
  private val rows = tile.rows

  // Define bounds outside of which we will consider the source as NoData
  private val westBound = extent.xmin
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  private val xmin = extent.xmin + re.cellwidth / 2.0
  private val xmax = extent.xmax - re.cellwidth / 2.0
  private val ymin = extent.ymin + re.cellheight / 2.0
  private val ymax = extent.ymax - re.cellheight / 2.0
  private val cellwidth = re.cellwidth
  private val cellheight = re.cellheight

  override def interpolate(x: Double, y: Double): Int =
    if (x < westBound || eastBound < x ||
      y < southBound || northBound < y) {
      NODATA // does raster have specific NODATA?
    } else {
      val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
      bilinearInt(leftCol, topRow, xRatio, yRatio)
    }

  def interpolateDouble(x: Double, y: Double): Double =
    if (x < westBound || eastBound < x ||
      y < southBound || northBound < y) {
      Double.NaN // does raster have specific NODATA?
    } else {
      val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
      bilinearDouble(leftCol, topRow, xRatio, yRatio)
    }

  def resolveTopLeftCoordsAndRatios(x: Double, y: Double): (Int, Int, Double, Double) = {
    val dleft = x - xmin
    val dright = xmax - x
    val dtop = ymax - y
    val dbottom = y - ymin

    val leftCol = math.floor(dleft / cellwidth).toInt
    val topRow = math.floor(dtop / cellheight).toInt

    val xRatio =
      if (dleft < 0) 1
      else if (dright < 0) 0
      else dleft / cellwidth - leftCol
    val yRatio =
      if (dtop < 0) 1
      else if (dbottom < 0) 0
      else dtop / cellheight - topRow

    (leftCol, topRow, xRatio, yRatio)
  }

  def bilinearInt(leftCol: Int, topRow: Int, xRatio: Double, yRatio: Double): Int =
    bilinear(leftCol, topRow, xRatio, yRatio, tile.get).round.toInt

  def bilinearDouble(leftCol: Int, topRow: Int, xRatio: Double, yRatio: Double): Double =
    bilinear(leftCol, topRow, xRatio, yRatio, tile.getDouble)

  private def bilinear(
    leftCol: Int,
    topRow: Int,
    xRatio: Double,
    yRatio: Double,
    f: (Int, Int) => Double): Double = {
    val (rightCol, bottomRow) = (leftCol + 1, topRow + 1)
    val (invXR, invYR) = (1 - xRatio, 1 - yRatio)

    var accum = 0.0
    var accumDivisor = 0.0

    if (leftCol >= 0 && topRow >= 0 && leftCol < cols && topRow < rows) {
      val mult = invXR * invYR
      accumDivisor += mult
      accum += f(leftCol, topRow) * mult
    }

    if (rightCol >= 0 && topRow >= 0 && rightCol < cols && topRow < rows) {
      val mult = (1 - invXR) * invYR
      accumDivisor += mult
      accum += f(rightCol, topRow) * mult
    }

    if (leftCol >= 0 && bottomRow >= 0 && leftCol < cols && bottomRow < rows) {
      val mult = invXR * (1 - invYR)
      accumDivisor += mult
      accum += f(leftCol, bottomRow) * mult
    }

    if (rightCol >= 0 && bottomRow >= 0 && rightCol < cols && bottomRow < rows) {
      val mult = (1 - invXR) * (1 - invYR)
      accumDivisor += mult
      accum += f(rightCol, bottomRow) * mult
    }

    accum / accumDivisor
  }

}
