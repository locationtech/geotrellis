package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._

object Reproject {
  /** Reprojection options.
    *
    * @param      method               The resampling method that will be used in this reprojection.
    * @param      errorThreshold       Error threshold when using approximate row transformations in reprojection.
    *                                  This default comes from GDAL 1.11 code.
    * @param      parentGridExtent     An optional GridExtent that if set represents the target grid extent for some
    *                                  parent window, which reprojected extents will snap to. Use with caution.
    * @param      targetCellSize       An optional cell size that if set will be used for for the projected raster.
    *                                  Use with caution.
    */
  case class Options(
    method: ResampleMethod = NearestNeighbor,
    errorThreshold: Double = 0.125,
    parentGridExtent: Option[GridExtent] = None,
    targetCellSize: Option[CellSize] = None
  )

  object Options {
    def DEFAULT = Options()

    implicit def methodToOptions(method: ResampleMethod): Options =
      apply(method = method)
  }
}
