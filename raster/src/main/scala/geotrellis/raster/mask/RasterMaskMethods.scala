package geotrellis.raster.mask

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Geometry, Extent}

/**
  * A trait containing extension methods related to masking of a
  * [[Raster]].
  */
trait RasterMaskMethods[T <: CellGrid] extends MethodExtensions[Raster[T]] {
  /**
    * Masks this raster by the given Geometry. Do not include polygon
    * exteriors.
    */
  def mask(geom: Geometry): Raster[T] =
    mask(Seq(geom), Options.DEFAULT)

  /**
    * Masks this raster by the given Geometry.
    */
  def mask(geom: Geometry, options: Options): Raster[T] =
    mask(Seq(geom), options)

  /**
    * Masks this raster by the given Geometry. Do not include polygon
    * exteriors.
    */
  def mask(geoms: Traversable[Geometry]): Raster[T] =
    mask(geoms, Options.DEFAULT)

  /**
    * Masks this raster by the given Geometry.
    */
  def mask(geoms: Traversable[Geometry], options: Options): Raster[T]
}
