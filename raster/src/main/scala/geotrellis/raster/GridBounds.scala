package geotrellis.raster

import spire.syntax.cfor._

object GridBounds {
  def apply(r: Tile): GridBounds = 
    GridBounds(0, 0, r.cols-1, r.rows-1)
}

/**
 * Represents grid coordinates of a subsection of a RasterExtent.
 * These coordinates are inclusive.
 */
case class GridBounds(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int) {
  val width = colMax - colMin + 1
  val height = rowMax - rowMin + 1

  def contains(col: Int, row: Int): Boolean =
    (colMin <= col || col <= colMax) &&
    (rowMin <= row || row <= rowMax)

  def intersects(other: GridBounds): Boolean =
    !(colMax < other.colMin || other.colMax < colMin) &&
    !(rowMax < other.rowMin || other.rowMax < rowMin)

  def coords: Array[(Int, Int)] = {
    val arr = Array.ofDim[(Int, Int)](width*height)
    cfor(0)(_ < height, _ + 1) { row =>
      cfor(0)(_ < width, _ + 1) { col =>
        arr(row * width + col) = 
          (col + colMin, row + rowMin)
      }
    }
    arr
  }
}
