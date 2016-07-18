package geotrellis.raster


/**
  * A grid composed of cells with specific value types
  */
trait CellGrid extends Grid {
  def cellType: CellType
}
