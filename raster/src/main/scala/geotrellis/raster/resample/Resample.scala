package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

sealed trait ResampleMethod

sealed trait PointResampleMethod extends ResampleMethod
case object NearestNeighbor  extends PointResampleMethod
case object Bilinear         extends PointResampleMethod
case object CubicConvolution extends PointResampleMethod
case object CubicSpline      extends PointResampleMethod
case object Lanczos          extends PointResampleMethod

sealed trait AggregateResampleMethod extends ResampleMethod
case object Average extends AggregateResampleMethod
case object Mode    extends AggregateResampleMethod


object ResampleMethod {
  val DEFAULT: PointResampleMethod = NearestNeighbor
}

abstract class Resample(tile: Tile, extent: Extent) {
  protected val re = RasterExtent(tile, extent)
  protected val cols = tile.cols
  protected val rows = tile.rows

  private val westBound = extent.xmin
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  protected val cellwidth = re.cellwidth
  protected val cellheight = re.cellheight

  /** Checks if a point is valid. We consider it valid if it is anywhere on or within the border. */
  private def isValid(x: Double, y: Double) =
    x >= westBound && x <= eastBound && y >= southBound && y <= northBound

  final def resample(x: Double, y: Double): Int =
    if (!isValid(x, y)) NODATA
    else resampleValid(x, y)

  final def resampleDouble(x: Double, y: Double): Double =
    if (!isValid(x, y)) Double.NaN
    else resampleDoubleValid(x, y)

  protected def resampleValid(x: Double, y: Double): Int

  protected def resampleDoubleValid(x: Double, y: Double): Double
}

abstract class AggregateResample(tile: Tile, extent: Extent, targetCS: CellSize) extends Resample(tile, extent) {

  def contributions(x: Double, y: Double): Seq[(Int, Int)] = {
    val halfWidth = targetCS.width / 2
    val halfHeight = targetCS.height / 2

    val leftIndex: Int = if(x - halfWidth < 0.0) 0
                         else (x - halfWidth).ceil.toInt
    val rightIndex: Int = if(x + halfWidth > cols - 1) cols - 1
                          else (x + halfWidth).floor.toInt
    val upperIndex: Int = if(y - halfHeight < 0.0) 0
                          else (y - halfHeight).ceil.toInt
    val lowerIndex: Int = if(y + halfHeight > rows - 1) rows - 1
                          else (y + halfHeight).floor.toInt

    for {
      xs <- leftIndex to rightIndex
      ys <- upperIndex to lowerIndex
    } yield (xs, ys)
  }
}

object Resample {
  def apply(method: PointResampleMethod, tile: Tile, extent: Extent): Resample =
    method match {
      case NearestNeighbor => new NearestNeighborResample(tile, extent)
      case Bilinear => new BilinearResample(tile, extent)
      case CubicConvolution => new BicubicConvolutionResample(tile, extent)
      case CubicSpline => new BicubicSplineResample(tile, extent)
      case Lanczos => new LanczosResample(tile, extent)
    }

  def apply(method: ResampleMethod, tile: Tile, extent: Extent, cs: CellSize): Resample =
    method match {
      case NearestNeighbor => new NearestNeighborResample(tile, extent)
      case Bilinear => new BilinearResample(tile, extent)
      case CubicConvolution => new BicubicConvolutionResample(tile, extent)
      case CubicSpline => new BicubicSplineResample(tile, extent)
      case Lanczos => new LanczosResample(tile, extent)
      case Average => new AverageResample(tile, extent, cs)
      case Mode => new ModeResample(tile, extent, cs)
    }
}
