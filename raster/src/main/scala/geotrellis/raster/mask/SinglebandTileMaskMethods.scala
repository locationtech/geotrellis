package geotrellis.raster.mask

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.{Mask, InverseMask}
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Geometry, Extent}


/**
  * A trait containing extension methods related to masking of
  * [[Tile]]s.
  */
trait SinglebandTileMaskMethods extends TileMaskMethods[Tile] {
  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster *are not* set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be empty -- all values
    * set to NODATA.
    */
  def localMask(r: Tile, readMask: Int, writeMask: Int): Tile =
    Mask(self, r, readMask, writeMask)

  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster is set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be identical to the first
    * raster.
    */
  def localInverseMask(r: Tile, readMask: Int, writeMask: Int): Tile =
    InverseMask(self, r, readMask, writeMask)

  /**
    * Masks this tile by the given Geometry.
    */
  def mask(ext: Extent, geoms: Traversable[Geometry], options: Options): Tile = {
    val re = RasterExtent(self, ext)
    val result = ArrayTile.empty(self.cellType, self.cols, self.rows)
    for (g <- geoms) {
      if (self.cellType.isFloatingPoint) {
        g.foreach(re, options)({ (col: Int, row: Int) =>
          result.setDouble(col, row, self.getDouble(col, row))
        })
      } else {
        g.foreach(re, options)({ (col: Int, row: Int) =>
          result.set(col, row, self.get(col, row))
        })
      }
    }
    result
  }
}
