package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.{Point, PointFeature, Extent}
class KrigingUniversalInterpolation(semivariogram: Function1[Double,Double], points: Seq[PointFeature[Int]], re: RasterExtent, pointPredict: Point, chunkSize: Int) {

}
