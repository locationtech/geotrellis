package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.{Point, PointFeature, Extent}

class KrigingOrdinaryInterpolation(semivariogram: Function1[Double,Double], points: Seq[PointFeature[Int]], re: RasterExtent, pointPredict: Point, chunkSize: Int) {

}
