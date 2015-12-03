package geotrellis.spark.mosaic

trait MergeMethods[T] extends Serializable {
  def merge(other: T): T
}
