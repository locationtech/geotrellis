package geotrellis.raster.mask

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.{Mask, InverseMask}
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector.{Geometry, Extent}


/**
  * A trait containing extension methods related to masking of
  * [[MultibandTile]]s.
  */
trait MultibandTileMaskMethods extends TileMaskMethods[MultibandTile] {
  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster *are not* set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be empty -- all values
    * set to NODATA.
    */
  def localMask(r: MultibandTile, readMask: Int, writeMask: Int): MultibandTile =
    ArrayMultibandTile(
      self.bands
        .zip(r.bands)
        .map { case (t, m) => t.localMask(m, readMask, writeMask) }
    )

  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster is set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be identical to the first
    * raster.
    */
  def localInverseMask(r: MultibandTile, readMask: Int, writeMask: Int): MultibandTile =
    ArrayMultibandTile(
      self.bands
        .zip(r.bands)
        .map { case (t, m) => t.localInverseMask(m, readMask, writeMask) }
    )

  /**
    * Masks this tile by the given Geometry.
    */
  def mask(ext: Extent, geoms: Traversable[Geometry], options: Options): MultibandTile =
    ArrayMultibandTile(
      self.bands.map(_.mask(ext, geoms, options))
    )
}
