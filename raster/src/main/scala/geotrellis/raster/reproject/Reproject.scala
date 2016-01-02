package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._

object Reproject {
  case class Options(
    method: ResampleMethod = NearestNeighbor,
    errorThreshold: Double = 0.125
  )

  object Options {
    def DEFAULT = Options()

    implicit def methodToOptions(method: ResampleMethod): Options =
      apply(method = method)
  }
}
