package geotrellis.spark.ingest

import geotrellis.raster.CellType

trait CellGridPrototype[T] {
  def prototype(cols: Int, rows: Int): T
  def prototype(cellType: CellType, cols: Int, rows: Int): T
}