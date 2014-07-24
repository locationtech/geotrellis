/**
 *
 */
package geotrellis

import geotrellis.raster.MultibandRasterData

/**
 * @author kelum
 *
 */

import geotrellis._

case class MultibandArrayRaster( data: MultibandRasterData, rasterExtent: RasterExtent) extends MultibandRaster{
  val raterType: RasterType = data.getType
  
  def getBand(index: Int): Raster = Raster(data.getBand(index), rasterExtent)
  def convert(typ:RasterType) = MultibandRaster(data.convert(typ), rasterExtent)
  def warp(target: RasterExtent) = MultibandArrayRaster(data.wrap(rasterExtent, target), target)
  def map(f:Int=>Int): MultibandRaster = MultibandRaster(data.map(f), rasterExtent)
}