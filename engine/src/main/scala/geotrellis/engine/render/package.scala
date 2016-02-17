package geotrellis.engine

import geotrellis.engine._

package object render {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class RenderRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends RenderRasterSourcePngMethods with RenderRasterSourceJpgMethods with RasterSourceColorMethods
}
