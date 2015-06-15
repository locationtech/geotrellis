package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.interpolation._
import spire.syntax.cfor._

object KrigingInterpolation {

  private def isValid(point: Point, re: RasterExtent): Boolean =
    point.x >= re.extent.xmin && point.x <= re.extent.xmax && point.y <= re.extent.ymax && point.y >= re.extent.ymin

  def apply(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Tile = {
    val cols = re.cols
    val rows = re.rows
    val tile = ArrayTile.alloc(TypeDouble, cols, rows)
    if(points.isEmpty) {
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
  }
}