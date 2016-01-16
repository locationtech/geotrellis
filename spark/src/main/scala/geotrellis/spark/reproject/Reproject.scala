package geotrellis.spark.reproject

import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.raster.resample._

object Reproject {
  case class Options(
    rasterReprojectOptions: RasterReprojectOptions = RasterReprojectOptions.DEFAULT,
    /** Attempts to match the total layer extent.
      * Warning: This should only be used on layers with smaller extents, and only
      * if you really need it to match what a reprojection would be on the parent
      * layer as one raster. Seams can happen on layers that use this that cover
      * too wide of an area.
      */
    matchLayerExtent: Boolean = false
  )

  object Options {
    def DEFAULT = Options()

    implicit def rasterReprojectOptionsToOptions(rro: RasterReprojectOptions): Options =
      Options(rasterReprojectOptions = rro)

   implicit def resampleMethodToOptions(method: ResampleMethod): Options =
      Options(rasterReprojectOptions = RasterReprojectOptions(method = method))
  }
}
