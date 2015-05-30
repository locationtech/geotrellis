package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._

/*
 * Input to the KrigingSimple => (semivariogram , points, re, pointPredict, chunkSize)
 * Should extend the KrigingInterpolation Class, which has to be made into abstract
 */

class KrigingSimpleInterpolation(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent,
                                 radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType)
      extends KrigingInterpolation(method, points, re, radius, chunkSize, lag, model){
  //override def kriging(semivariogram: Function1[Double,Double], points: Seq[PointFeature[Int]], re: RasterExtent, pointPredict: Point, chunkSize: Int): Tile = {

  //private lazy val prediction = krigingsimple(NODATA, tile.get)

  def krigingsimple(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], pointPredict: Point, chunkSize: Int, lag:Int=0, model:ModelType): Tile = {
    val semivariogram = Semivariogram(points, radius, lag, model)
    /*
     * After processing a Tile is returned corresponding to the "Point" provided
     */
    ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  }
  override def interpolateValid(point: Point): Tile = krigingsimple(method, points, re, radius, point, chunkSize, lag, model)
}

//object KrigingSimpleInterpolation()
