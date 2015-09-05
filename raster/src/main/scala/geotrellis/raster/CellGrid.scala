package geotrellis.raster

trait CellGrid extends Serializable {
  def cols: Int
  def rows: Int
  def cellType: CellType

  def dimensions: (Int, Int) = (cols, rows)
  def gridBounds: GridBounds = GridBounds(0, 0, cols - 1, rows - 1)
  def size = cols * rows
}
