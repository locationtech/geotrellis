package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.interpolation._
import spire.syntax.cfor._

object KrigingInterpolation {

  private def isValid(point: Point, re: RasterExtent): Boolean =
    point.x >= re.extent.xmin && point.x <= re.extent.xmax && point.y <= re.extent.ymax && point.y >= re.extent.ymin

  def apply(method: KrigingVectorInterpolationMethod, points: Seq[PointFeature[Double]], re: RasterExtent, maxdist: Double, binmax: Double, model: ModelType): Tile = {
    model match {
      case Linear => throw new UnsupportedOperationException("Linear semivariogram does not accept maxDist and maxBin values")
      case _ =>
        val cols = re.cols
        val rows = re.rows
        val tile = ArrayTile.alloc(TypeDouble, cols, rows)
        if(points.isEmpty)
          throw new UnsupportedOperationException("The set of points for constructing the prediction is empty")
        else {
          val rasterData: Array[Point] = Array.tabulate(rows * cols){i => Point(i/cols, i%cols)}
          val prediction: Array[(Double, Double)] = method.predict(rasterData)
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              tile.setDouble(col, row, prediction(row*cols + col)._1)
            }
          }
          tile
        }
    }
  }

  def apply(method: KrigingVectorInterpolationMethod, points: Seq[PointFeature[Double]], re: RasterExtent, radius: Option[Double], chunkSize: Double, lag: Double = 0, model: ModelType): Tile = {
    model match {
      case Linear =>
        val cols = re.cols
        val rows = re.rows
        val tile = ArrayTile.alloc(TypeDouble, cols, rows)
        if (points.isEmpty) {
          println("The set of points for constructing the prediction is empty")
          tile
        } else {
          //val funcInterp = method.createPredictor(points, radius, chunkSize, lag, model, method)
          val funcInterp = method.createPredictor()
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val (x, y) = re.gridToMap(col, row)
              val (v, _) = funcInterp(Point(x, y))

              tile.setDouble(col, row, v)
            }
          }
          tile
        }
      case _ => throw new UnsupportedOperationException("Non linear semivariograms do not accept radii and lags")
    }
  }
}