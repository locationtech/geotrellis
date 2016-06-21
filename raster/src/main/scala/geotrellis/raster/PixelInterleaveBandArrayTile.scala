package geotrellis.raster

import spire.syntax.cfor._

object PixelInterleaveBandArrayTile {
  def apply(inner: ArrayTile, bandCount: Int, bandIndex: Int): PixelInterleaveBandArrayTile =
    new PixelInterleaveBandArrayTile(inner, bandCount, bandIndex)
}

/**
  * This [[Tile]] type allows for an ArrayTile that contains pixel values as a pixel interleaved
  * set (meaning each column has one value for each band in a row) to be viewed as a
  * Tile for a specific band.
  *
  * @param   inner         ArrayTile that holds the inner data in pixel interleave format.
  * @param   bandCount     The number of bands represented in the inner tile. inner.cols / bandCount must be a natural number.
  * @param   bandIndex     The index of the band this tile should represent.
  */
class PixelInterleaveBandArrayTile(inner: ArrayTile, bandCount: Int, bandIndex: Int) extends ArrayTile {
  def cellType: CellType = inner.cellType
  def cols: Int = inner.cols / bandCount
  def rows: Int = inner.rows

  def apply(i: Int): Int = inner.apply(i * bandCount + bandIndex)
  def applyDouble(i: Int): Double = inner.applyDouble(i * bandCount + bandIndex)
  def copy: ArrayTile = mutable

  def toBytes(): Array[Byte] = mutable.toBytes

  def mutable: MutableArrayTile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    if(cellType.isFloatingPoint) {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.setDouble(col, row, getDouble(col, row))
        }
      }
    } else {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.set(col, row, get(col, row))
        }
      }
    }

    tile
  }
}
