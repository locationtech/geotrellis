package geotrellis.raster.reproject

abstract sealed class InterpolationMethod

import geotrellis.raster._
import geotrellis.vector.Extent

case object NearestNeighbor extends InterpolationMethod
case object Bilinear extends InterpolationMethod
case object Cubic extends InterpolationMethod
case object CubicSpline extends InterpolationMethod
case object Lanczos extends InterpolationMethod
case object Average extends InterpolationMethod
case object Mode extends InterpolationMethod


trait Interpolation {
  def interpolate(x: Double, y: Double): Int
  def interpolateDouble(x: Double, y: Double): Double
}

object Interpolation {
  def apply(method: InterpolationMethod, tile: Tile, extent: Extent): Interpolation =
    method match {
      case NearestNeighbor => new NearestNeighborInterpolation(tile, extent)
      case Bilinear => new BilinearInterpolation(tile, extent)
      case Cubic => ???
      case CubicSpline => ???
      case Lanczos => ???
      case Average => ???
      case Mode => ???
    }
}

class NearestNeighborInterpolation(tile: Tile, extent: Extent) extends Interpolation {
  val re = RasterExtent(tile, extent)

  def interpolate(x: Double, y: Double): Int = {
    val col = re.mapXToGrid(x)
    val row = re.mapYToGrid(y)
    tile.get(col, row)
  }

  def interpolateDouble(x: Double, y: Double): Double = {
    val col = re.mapXToGrid(x)
    val row = re.mapYToGrid(y)
    tile.getDouble(col, row)
  }

}

class BilinearInterpolation(tile: Tile, extent: Extent) extends Interpolation {
  val re = RasterExtent(tile, extent)

  // Define bounds outside of which we will consider the source as NoData
  private val westBound = extent.xmin
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  private val xmin = extent.xmin + (re.cellwidth / 2.0)
  private val xmax = extent.xmax - (re.cellwidth / 2.0)
  private val ymin = extent.ymin + (re.cellheight / 2.0)
  private val ymax = extent.ymax - (re.cellheight / 2.0)
  private val cellwidth = re.cellwidth
  private val cellheight = re.cellheight
  private val inverseDeltas = 1 / (cellwidth * cellheight)

  def interpolate(x: Double, y: Double): Int = 
    if(x < westBound || eastBound < x ||
       y < southBound || northBound < y) {
      NODATA
    } else {
      val dleft = x - xmin
      val leftCol = (dleft / cellwidth).toInt
      val leftX = xmin + (leftCol * cellwidth)

      val dright = xmax - x
      val rightCol = leftCol + 1
      val rightX = leftX + cellwidth

      val dbottom = y - ymin
      val bottomRow = (dbottom / cellheight).toInt
      val bottomY = ymin + (bottomRow * cellheight)

      val dtop = ymax - y
      val topRow = bottomRow + 1
      val topY = bottomY + cellheight

      (inverseDeltas *
      ( (tile.get(leftCol, topRow) * (dright * dbottom) ) +
        (tile.get(rightCol, topRow) * (dleft * dbottom) ) +
        (tile.get(leftCol, bottomRow) * (dright * dtop) ) +
        (tile.get(rightCol, bottomRow) * (dleft * dtop) ) )).toInt
    }

  def interpolateDouble(x: Double, y: Double): Double =
    if(x < westBound || eastBound < x ||
       y < southBound || northBound < y) {
      Double.NaN
    } else {
      val dleft = x - xmin
      val leftCol = (dleft / cellwidth).toInt
      val leftX = xmin + (leftCol * cellwidth)

      val dright = xmax - x
      val rightCol = leftCol + 1
      val rightX = leftX + cellwidth

      val dbottom = y - ymin
      val bottomRow = (dbottom / cellheight).toInt
      val bottomY = ymin + (bottomRow * cellheight)

      val dtop = ymax - y
      val topRow = bottomRow + 1
      val topY = bottomY + cellheight

      inverseDeltas *
      ( (tile.getDouble(leftCol, topRow) * (dright * dbottom) ) +
        (tile.getDouble(rightCol, topRow) * (dleft * dbottom) ) +
        (tile.getDouble(leftCol, bottomRow) * (dright * dtop) ) +
        (tile.getDouble(rightCol, bottomRow) * (dleft * dtop) ) )
    }
}
