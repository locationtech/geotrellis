package geotrellis.raster.prototype

import geotrellis.raster._
import geotrellis.util.MethodExtensions


/**
  * Methods that allow us to create a similar tile type from an
  * already existing tile.
  */
trait TilePrototypeMethods[T <: CellGrid] extends MethodExtensions[T] {

  /**
    * Given numbers of columns and rows, produce a new [[ArrayTile]]
    * of the given size and the same band count as the calling object.
    */
  def prototype(cols: Int, rows: Int): T

  /**
    * Given a [[CellType]] and numbers of columns and rows, produce a
    * new [[ArrayTile]] of the given size and the same band count as
    * the calling object.
    */
  def prototype(cellType: CellType, cols: Int, rows: Int): T
}
