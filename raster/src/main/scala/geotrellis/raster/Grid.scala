package geotrellis.raster


trait Grid extends Serializable {
  def cols: Int
  def rows: Int

  /**
    * The size of the grid, e.g. cols * rows.
    */
  def size = cols * rows
  def dimensions: (Int, Int) = (cols, rows)
  def gridBounds: GridBounds = GridBounds(0, 0, cols - 1, rows - 1)
}
