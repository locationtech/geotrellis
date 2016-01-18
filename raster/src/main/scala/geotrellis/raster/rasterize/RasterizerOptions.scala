package geotrellis.raster.rasterize

import geotrellis.raster._


object Rasterize {
  case class Options(
    includePartial: Boolean,
    sampleType: PixelSampleType
  )

  object Options {
    def DEFAULT = Options(includePartial = true, sampleType = PixelIsPoint)
  }
}
