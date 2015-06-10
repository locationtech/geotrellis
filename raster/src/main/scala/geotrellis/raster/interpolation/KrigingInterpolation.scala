package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.interpolation._
import spire.syntax.cfor._

abstract sealed class KrigingInterpolationMethod

case object KrigingSimple extends KrigingInterpolationMethod
case object KrigingOrdinary extends KrigingInterpolationMethod
case object KrigingUniversal extends KrigingInterpolationMethod
case object KrigingGeo extends KrigingInterpolationMethod

object KrigingInterpolationMethod {
  val DEFAULT = KrigingSimple
  //val DEFAULT = KrigingOrdinary
}

object KrigingInterpolation {

  private def isValid(point: Point, re: RasterExtent): Boolean =
    point.x >= re.extent.xmin && point.x <= re.extent.xmax && point.y <= re.extent.ymax && point.y >= re.extent.ymin

  private def getfuncInterp(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType, method: KrigingInterpolationMethod) = method match {
      case KrigingSimple =>
        KrigingVectorInterpolation(KrigingVectorSimple, points, radius, chunkSize, lag, model)

      case KrigingOrdinary =>
        KrigingVectorInterpolation(KrigingVectorOrdinary, points, radius, chunkSize, lag, model)

      case KrigingUniversal =>
        KrigingVectorInterpolation(KrigingVectorUniversal, points, radius, chunkSize, lag, model)

      case KrigingGeo =>
        KrigingVectorInterpolation(KrigingVectorGeo, points, radius, chunkSize, lag, model)
    }

  private def kriging(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType, method: KrigingInterpolationMethod): Tile = {
    val cols = re.cols
    val rows = re.rows
    val tile = ArrayTile.alloc(TypeDouble, cols, rows)
    if(points.isEmpty) {
      println("The set of points for constructing the prediction is empty")
      tile
    } else {
      def funcInterp = getfuncInterp(points, radius, chunkSize, lag, model, method)
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

  def apply(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Tile = {
    kriging(points, re, radius, chunkSize, lag, model, method)
  }
}