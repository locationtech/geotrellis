package geotrellis.spark.mosaic

trait MergeMethods[T] {
  def merge(other: T): T
}
