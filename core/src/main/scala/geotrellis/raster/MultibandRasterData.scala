/**
 *
 */
package geotrellis.raster

/**
 * @author kelum
 *
 */

import geotrellis._

case class MultibandRasterData(arr: Array[RasterData]) {
  def getBand(index: Int): RasterData = arr(index)
  def getType: RasterType = arr(0).getType
  def bands: Int = arr.length
  def cols: Int = arr(0).cols
  def rows: Int = arr(0).rows
  def convert(typ: RasterType): MultibandRasterData = MultibandRasterData(arr.map { e => e.convert(typ) })
  def wrap(current: RasterExtent, target: RasterExtent): MultibandRasterData = MultibandRasterData(arr.map { e => e.warp(current, target)})
  def map(f: Int => Int): MultibandRasterData = MultibandRasterData(arr.map(e => e.map(f)))
}