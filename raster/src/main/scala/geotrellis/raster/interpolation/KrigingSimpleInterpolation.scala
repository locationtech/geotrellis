package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import breeze.linalg._

class KrigingSimpleInterpolation(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent,
                                 radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType)
      extends KrigingInterpolation(method, points, re, radius, chunkSize, lag, model){

  def krigingsimple(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], pointPredict: Point, chunkSize: Int, lag:Int=0, model:ModelType): Tile = {
    val semivariogram = Semivariogram(points, radius, lag, model)
    //val x = DenseVector.zeros[Double](5)
    ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  }
  override def interpolateValid(point: Point): Tile = {
    krigingsimple(method, points, re, radius, point, chunkSize, lag, model)
  }
}
