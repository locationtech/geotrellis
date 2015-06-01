package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import spire.syntax.cfor._

abstract sealed class KrigingInterpolationMethod
case object KrigingSimple extends KrigingInterpolationMethod
case object KrigingOrdinary extends KrigingInterpolationMethod
case object KrigingUniversal extends KrigingInterpolationMethod
case object KrigingGeo extends KrigingInterpolationMethod


object KrigingInterpolationMethod {
  val DEFAULT = KrigingOrdinary
}

abstract class KrigingInterpolation(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent,
                           radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType) {
  protected def kriging(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType): Tile = ???

  private def isValid(point: Point) =
    point.x >= re.extent.xmin && point.x <= re.extent.xmax && point.y <= re.extent.ymax && point.y >= re.extent.ymin

  final def interpolate(point: Point): Tile =
    if (!isValid(point)) throw new IllegalArgumentException("Point out of the raster range");
    else interpolateValid(point)

  protected def interpolateValid(point: Point): Tile = ???

  /*
  def constructSV(points:Seq[PointFeature[Int]], radius:Option[Int]=None, lag:Int=0, model:ModelType):Function1[Double,Double] = {
    Semivariogram(points, radius, lag, model)
  }
  protected def kriging(semivariogram: Function1[Double, Double], points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], pointPredict: Point, chunkSize: Int, lag:Int=0, model:ModelType): Tile
  val sv = constructSV(points, radius, lag, model)
  */
}

object KrigingInterpolation {
  def apply(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType) = {
    method match {
      case KrigingSimple => new KrigingSimpleInterpolation(method, points, re, radius, chunkSize, lag, model)
      /*
       * After this, create the other four implementations incrementally
       *
      */
      case KrigingOrdinary => ???
      case KrigingUniversal => ???
      case KrigingGeo => ???

      /*  With the semivariogram precomputed in the val in current class
       *  To decide the way in which to go proceed
      val semivariogram = Semivariogram(points, radius, lag, model)
      case KrigingSimple => new KrigingSimpleInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      case KrigingOrdinary => new KrigingOrdinaryInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      case KrigingUniversal => new KrigingUniversalInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      case KrigingGeo => new KrigingGeoInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      */
    }
  }
}