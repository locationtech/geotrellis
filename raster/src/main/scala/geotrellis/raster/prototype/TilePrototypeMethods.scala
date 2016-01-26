package geotrellis.raster.prototype

import geotrellis.raster._

/** Methods that allow us to create a similar tile type from an already existing tile. */
trait TilePrototypeMethods[T <: CellGrid] extends MethodExtensions[T] {
  def prototype(cols: Int, rows: Int): T
  def prototype(cellType: CellType, cols: Int, rows: Int): T
}
