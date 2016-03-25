package geotrellis.raster.rasterize

import geotrellis.raster._


/**
  * An object which houses rasterizer options.
  */
object Rasterize {

  /**
    * A type encoding rasterizer options.
    */
  case class Options(
    includePartial: Boolean,
    sampleType: PixelSampleType
  )

  /**
    * A companion object for the [[Options]] type.  Includes a
    * function to produce the default options settings.
    */
  object Options {
    def DEFAULT = Options(includePartial = true, sampleType = PixelIsPoint)
  }
}
