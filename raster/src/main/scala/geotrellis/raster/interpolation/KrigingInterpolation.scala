package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.interpolation.{KrigingVectorSimple, KrigingVectorInterpolation}
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

  private def krigingSimple(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Tile = {
    val cols = re.cols
    val rows = re.rows
    val tile = ArrayTile.alloc(TypeDouble, cols, rows)
    if(points.isEmpty) {
      println("The set of points for constructing the prediction is empty")
      tile
    } else {
      model match {
        case Linear =>
          def funcInterp = KrigingVectorInterpolation(KrigingVectorSimple, points, radius, chunkSize, lag, model)
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val (x, y) = re.gridToMap(col, row)
              val (v, _) = funcInterp(Point(x, y))
              //Visualizing the mapping
              //println("(x, y) = (" + x + ", " + y + "), (col, row) = (" + col + ", " + row + ") => " + v)
              tile.setDouble(col, row, v)
            }
          }
          //Visualizing the prediction wrt the mapping
          //println("Rows = " + rows + "\n Cols = " + cols)
          //println(tile.asciiDraw())
          //println(points.mkString("\n"))
        case _ => ???
      }
      tile
    }
  }

  private def krigingOrdinary(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Tile = ???
  private def krigingUniversal(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Tile = ???
  private def krigingGeo(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Tile = ???

  def apply(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Tile = {
    method match {
      case KrigingSimple => krigingSimple(points, re, radius, chunkSize, lag, model)
      case KrigingOrdinary => krigingOrdinary(points, re, radius, chunkSize, lag, model)
      case KrigingUniversal => krigingUniversal(points, re, radius, chunkSize, lag, model)
      case KrigingGeo => krigingGeo(points, re, radius, chunkSize, lag, model)
      case _ => {
        println("The Kriging type is not recognized, using default Kriging type")
        apply(KrigingInterpolationMethod.DEFAULT, points, re, radius, chunkSize, lag, model)
      }
    }
  }
}