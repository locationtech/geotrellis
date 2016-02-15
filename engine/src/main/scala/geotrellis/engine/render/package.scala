package geotrellis.engine

import geotrellis.engine._

package object render {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class RenderRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends RenderRasterSourcePngMethods with RenderRasterSourceJpgMethods with RasterSourceColorMethods
}
