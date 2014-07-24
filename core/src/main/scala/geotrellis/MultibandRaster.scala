/**
 *
 */
package geotrellis

/**
 * @author kelum
 *
 */

import geotrellis.raster._

object MultibandRaster {
  def apply(arr: MultibandRasterData, re: RasterExtent): MultibandRaster =
    MultibandArrayRaster(arr, re)
}

/**
 * Base trait for the Multiband Raster data type.
 */

trait MultibandRaster {
  def getBand(index: Int): Raster
  def convert(typ: RasterType): MultibandRaster
  def warp(target: RasterExtent)
  def map(f:Int=>Int): MultibandRaster
}