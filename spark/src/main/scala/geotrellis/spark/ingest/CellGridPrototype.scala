package geotrellis.spark.ingest

trait CellGridPrototype[T] {
  def prototype(cols: Int, rows: Int): T
}