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
  /*
   *  val DEFAULT = KrigingOrdinary
   *  To be changed to this after the structure is set up
   */
  val DEFAULT = KrigingSimple
}

abstract class KrigingInterpolation(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent,
                           radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType) {
  protected def kriging(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType): Tile = ???

  //Implement the raster checking of the interpolation query
  final def interpolate(point: Point): Tile =
    interpolateValid(point)
    //kriging(point)

  //To configure this into checking the point and then invoking the interpolation method
  protected def interpolateValid(point: Point): Tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)

  /*
  def constructSV(points:Seq[PointFeature[Int]], radius:Option[Int]=None, lag:Int=0, model:ModelType):Function1[Double,Double] = {
    Semivariogram(points, radius, lag, model)
  }

  //final def kriging(semivariogram: Function1[Double,Double], points: Seq[PointFeature[Int]], re: RasterExtent, pointPredict: Point, chunkSize: Int): Tile = ???
  //protected def kriging(semivariogram: Function1[Double, Double], points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], pointPredict: Point, chunkSize: Int, lag:Int=0, model:ModelType): Tile
  //val sv = constructSV(points, radius, lag, model)
  //Semivariogram(points,None,2,Linear)
  //override def interpolateValid(x: Double, y: Double): Int = ???
  //override def interpolateDoubleValid(x: Double, y: Double): Double = ???
  */

}

object KrigingInterpolation {
  def apply(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType) = {
    //val semivariogram = Semivariogram(points, radius, lag, model)
    method match {
      case KrigingSimple => new KrigingSimpleInterpolation(method, points, re, radius, chunkSize, lag, model)
      /*
       * After this, create the other four implementations incrementally
       *
      case KrigingOrdinary => new KrigingOrdinaryInterpolation(method, points, re, radius, chunkSize, lag, model)
      case KrigingUniversal => new KrigingUniversalInterpolation(method, points, re, radius, chunkSize, lag, model)
      case KrigingGeo => new KrigingGeoInterpolation(method, points, re, radius, chunkSize, lag, model)
       */

      /*  With the semivariogram precomputed in the val in current class
       *  To decide the way in which to go proceed
      case KrigingSimple => new KrigingSimpleInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      case KrigingOrdinary => new KrigingOrdinaryInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      case KrigingUniversal => new KrigingUniversalInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      case KrigingGeo => new KrigingGeoInterpolation(semivariogram , points, re, pointPredict, chunkSize)
      */
    }
  }
}