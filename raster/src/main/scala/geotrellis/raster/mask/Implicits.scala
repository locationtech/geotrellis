package geotrellis.raster.mask

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector.Geometry

object Implicits extends Implicits

trait Implicits {
  implicit class withRasterMaskMethods[T <: CellGrid: (? => TileMaskMethods[T])](val self: Raster[T]) extends RasterMaskMethods[T] {
    /**
      * Masks this raster by the given Geometry.
      */
    def mask(geoms: Traversable[Geometry], options: Options): Raster[T] =
      self.mapTile(_.mask(self.extent, geoms, options))
  }
}
